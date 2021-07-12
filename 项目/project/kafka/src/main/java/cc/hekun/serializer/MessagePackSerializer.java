package cc.hekun.serializer;

import cc.hekun.action.SerializerMethod;
import cc.hekun.annotation.Ignore;
import lombok.val;
import org.apache.log4j.Logger;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;


/**
 * msgPack v0.8.22 序列化
 * @author hekun / hekun.cc / kunscomputer@foxmail.com
 * 任何数据量情况下序列化反序列化效率远远高于json,jdk方式,中等数据量(2000对象左右,测试对象下面有)情况下也优于hessian
 * 编码字节数远远小于json,jdk,hessian 三种序列化方式。
 * 在存储基本类型或基本类型集合的情况下,序列化反序列化速度远远高于其他三种序列化方式,大小相比jdk,json有绝对优势,但和hessian比大小完全一样！大小永远差距只有2。
 * 关于使用
 * 实体类型请保持一致
 * 应该使用基本类型的bean , 现在支持直接传入List和基本类型的数据,但是 ,无法处理List内部泛型(对象数组)为List的情况
 * 不支持传入数组类型[]的任何数据,如果有需要可以自行添加处理方案
 * 可以在对象内部存在List/ArrayList/LinkedList 参数,集合的泛型必须是普通对象(基本类型包装类或bean实体),不能是List类型
 * 因为特殊的顺序赋值方式,其实实体的变量名可以不需要对应,只需要顺序对应。
 * 提供了特殊的转换机制,序列化对象中的int等基本类型字段,对应反序列化对象的Integer字段,反之则依赖jdk默认初始化的赋值机制,Integer等包装类对应的基本类型也会初始化为默认值
 *
 * 如果有复杂对象,比如Date类等,请不要使用此序列化方式(或用其他方式 比如把时间变为字符串格式)。
 * 能通过@Ignore注解声明不需要被序列化的属性。
 *
 * 关于压缩的实现完全依据官方github最新版本文档,此方法封装了对象的处理方式。
 *
 *
 * @date 2021年6月7日 14点01分 更新
 * 提供新的序列化反序列化重载方法,现在序列化反序列化不需要提供类型参数,直接传入对象或者字节数组即可
 *
 *
 *
 * @date 2021年6月16日 14点11分
 * 使用kafka整合时发现一个问题,就是基本类型应该用约定的字节代表类型以节省空间,现在已经修改,基本类型只占一个字节空间表示类型
 *
 *
 * @author ouyanghekun
 * @date 2021-06-04 15:07
 */
public class MessagePackSerializer extends SerializerMethod {

    /**
     * 日志
     */
    private static final Logger logger = Logger.getLogger(MessagePackSerializer.class);

    /**
     * 因为是static修饰,初始化代码块也是static的,jvm保证初始化线程安全
     * 存放基本类型class和数字的对应集合
     */
    private static final Map<Class<?>, Integer> BASIC_TYPE_VALUES = new HashMap<>();
    /**
     * 基本类型转化异常默认值
     */
    private static final Map<Class<?>, Object> BASIC_TYPE_INIT_VALUE = new HashMap<>();

    /**
     *
     */
    private static final Map<String , Class<?>> BASIC_TYPE_CLASS = new HashMap<>();

    @Override
    public byte[] doSerialize(Object obj) {
        if(obj instanceof List){
            return serializeByMsgPackList(cast(obj));
        }
        MessageBufferPacker messageBufferPacker = null;
        byte[] res = null;
        //找出所有属性
        try {
            int type = getIntegerValue(obj.getClass());
            messageBufferPacker = MessagePack.newDefaultBufferPacker();
            //约定 字节数组第一个位置放类型
            messageBufferPacker.packInt(type);
            //约定 字节数组第二个位置存放全类名(基本类型除外)
            //done 这里基本类型不应该放全类名，太占用空间了，应该用约定的字符(已完成)
            messageBufferPacker.packString(getTypeValue(obj));
            if(type == -1){
                putFields(obj, messageBufferPacker);
            }else {
                putBasicValue(messageBufferPacker,type,obj);
            }
            messageBufferPacker.flush();
            res = messageBufferPacker.toByteArray();
        } catch (Exception e) {
            logger.error("msgPack序列化异常", e);
        } finally {
            try {
                if (!Objects.isNull(messageBufferPacker)) {
                    messageBufferPacker.close();
                }
            } catch (IOException e) {
                logger.error("messageBufferPacker流关闭异常", e);
            }
        }
        return res;
    }

    @Override
    public Object deSerialize(byte[] objectByteArray, Class<?> clazz) {
        Object resObj = null;
        MessageUnpacker messageUnpacker = null;
        try {
            messageUnpacker = MessagePack.newDefaultUnpacker(objectByteArray);
            //约定,第一个位置（2字节）必是类型
            int type = messageUnpacker.unpackInt();
            //处理集合类型
            if(type == BasicTypeEnum.LIST.value){
                return deSerializeWithListType(clazz,messageUnpacker);
            }
            //反射创建对象,先留着魔法值
            if(type == -1){
                resObj = clazz.newInstance();
                setFields(resObj, messageUnpacker);
            }else{
                //单一基本类型特殊处理
                resObj = cast(getBasicValue(messageUnpacker,type));
            }
        } catch (Exception e) {
            logger.error("msgPack反序列化异常", e);
        } finally {
            try {
                if (!Objects.isNull(messageUnpacker)) {
                    messageUnpacker.close();
                }
            } catch (IOException e) {
                logger.error("messageBufferPacker流关闭异常", e);
            }
        }
        return resObj;
    }


    @Override
    public Object deSerialize(byte[] objectByteArray) {
        Object resObj = null;
        MessageUnpacker messageUnpacker = null;
        try {
            messageUnpacker = MessagePack.newDefaultUnpacker(objectByteArray);
            //约定,第一个位置（2字节）必是类型
            int type = messageUnpacker.unpackInt();
            //第二个位置是字符串类型
            String className = messageUnpacker.unpackString();
            if("".equals(className)){
                //此时肯定为空集合
                return new ArrayList<>();
            }
            Class clazz = getClassByString(className);
            //处理集合类型
            if(type == BasicTypeEnum.LIST.value){
                return deSerializeWithListType(clazz,messageUnpacker);
            }
            //反射创建对象,先留着魔法值
            if(type == -1){
                resObj = clazz.newInstance();
                setFields(resObj, messageUnpacker);
            }else{
                //单一基本类型特殊处理
                resObj = cast(getBasicValue(messageUnpacker,type));
            }
        } catch (Exception e) {
            logger.error("msgPack反序列化异常", e);
        } finally {
            try {
                if (!Objects.isNull(messageUnpacker)) {
                    messageUnpacker.close();
                }
            } catch (IOException e) {
                logger.error("messageBufferPacker流关闭异常", e);
            }
        }
        return resObj;
    }


    /**
     * 集合反序列化
     * @param clazz clazz
     * @param messageUnpacker unpacker
     * @return
     */
    public Object deSerializeWithListType( Class<?> clazz , MessageUnpacker messageUnpacker) {
        List<Object> res = null;
        try {
            int type = getIntegerValue(clazz);
            //通过约定拿到长度
            int size = messageUnpacker.unpackInt();
            res = new ArrayList<>(size);
            //批量解析对象
            if(type == -1){
                for(int i = 0 ; i < size ;i++){
                    Object resObj = clazz.newInstance();
                    setFields(resObj, messageUnpacker);
                    res.add(resObj);
                }
            }else{
                //基本类型集合赋值
                getBasicValue(messageUnpacker,type,size,cast(res));
            }
        } catch (Exception e) {
            logger.error("msgPack反序列化异常", e);
        } finally {
            try {
                if (!Objects.isNull(messageUnpacker)) {
                    messageUnpacker.close();
                }
            } catch (IOException e) {
                logger.error("messageBufferPacker流关闭异常", e);
            }
        }
        return res;
    }

    @Override
    public Object deSerializeWithListType(byte[] objectByteArray, Class<?> clazz) {
        List<Object> res = null;
        MessageUnpacker messageUnpacker = null;
        try {
            int type = getIntegerValue(clazz);
            messageUnpacker = MessagePack.newDefaultUnpacker(objectByteArray);
            //通过约定拿到长度
            int size = messageUnpacker.unpackInt();
            res = new ArrayList<>(size);
            //批量解析对象
            if(type == -1){
                for(int i = 0 ; i < size ;i++){
                    Object resObj = clazz.newInstance();
                    setFields(resObj, messageUnpacker);
                    res.add(resObj);
                }
            }else{
                //基本类型集合赋值
                getBasicValue(messageUnpacker,type,size,cast(res));
            }
        } catch (Exception e) {
            logger.error("msgPack反序列化异常", e);
        } finally {
            try {
                if (!Objects.isNull(messageUnpacker)) {
                    messageUnpacker.close();
                }
            } catch (IOException e) {
                logger.error("messageBufferPacker流关闭异常", e);
            }
        }
        return res;
    }

    /**
     * 序列化集合类型对象
     * @param listObj 集合对象
     * @return 返回序列化字节数组
     */
    public  byte[] serializeByMsgPackList(List<Object> listObj){
        MessageBufferPacker messageBufferPacker = null;
        byte[] res = null;
        //空列表直接返回空
        if(Objects.isNull(listObj)){
            return null;
        }
        //找出所有属性
        try {
            messageBufferPacker = MessagePack.newDefaultBufferPacker();
            int type = -1;
            //约定,第一个数代表类型,用于解包时使用
            messageBufferPacker.packInt(BasicTypeEnum.LIST.value);
            int size = listObj.size();
            if(size > 0){
                //约定,第二个数,获取类型全类名保存,空集合不保存类型
                messageBufferPacker.packString(getTypeValue(listObj.get(0)));
                //设置类型
                type = getIntegerValue(listObj.get(0).getClass());
            }else {
                //花费1字节记录空字符串
                messageBufferPacker.packString("");
            }
            //约定,第三个数字为集合长度
            messageBufferPacker.packInt(size);
            //空集合
            if(type == -1){
                //处理普通pojo对象
                for(Object oneObj: listObj){
                    putFields(oneObj, messageBufferPacker);
                }
            }else{
                //处理基本实体类
                putBasicValue(messageBufferPacker,type,size,listObj);
            }
            messageBufferPacker.flush();
            res = messageBufferPacker.toByteArray();
        } catch (Exception e) {
            logger.error("msgPack序列化异常", e);
        } finally {
            try {
                if (!Objects.isNull(messageBufferPacker)) {
                    messageBufferPacker.close();
                }
            } catch (IOException e) {
                logger.error("messageBufferPacker流关闭异常", e);
            }
        }
        return res;
    }


    /**
     * 设置对象所有的属性值
     *
     * @param obj             对象实例
     * @param messageUnpacker 编码器
     * @throws Exception 异常
     */
    private void setFields(Object obj, MessageUnpacker messageUnpacker) throws Exception {
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            //跳过指定字段的序列化
            if (field.getAnnotation(Ignore.class) != null) {
                continue;
            }
            //基本数据类型直接打包,扩展类型递归处理
            setValue(messageUnpacker, field, obj);
        }
    }

    /**
     * 在buffer中添加值
     *
     * @param messageUnpacker buffer
     * @param field           字段
     * @param obj             对象实例
     * @throws Exception 异常
     */
    private void setValue(MessageUnpacker messageUnpacker, Field field, Object obj) throws Exception {
        //如果下一个值为空值,对空值解包
        if (messageUnpacker.tryUnpackNil()) {
            //属性不用赋值
            return;
        }
        switch (getIntegerValue(field.getType())) {
            case 1:
                field.set(obj, messageUnpacker.unpackBoolean());
                break;
            case 2:
                field.set(obj, messageUnpacker.unpackByte());
                break;
            case 3:
                field.set(obj, messageUnpacker.unpackShort());
                break;
            case 4:
                field.set(obj, messageUnpacker.unpackInt());
                break;
            case 5:
                field.set(obj, messageUnpacker.unpackString());
                break;
            case 6:
                field.set(obj, messageUnpacker.unpackLong());
                break;
            case 7:
                field.set(obj, messageUnpacker.unpackDouble());
                break;
            case 11:
                field.set(obj, messageUnpacker.unpackFloat());
                break;
            case 9:
                List<Object> list = new ArrayList<>();
                field.set(obj, list);
                //对集合元素赋值
                setListValue(list, messageUnpacker);
                break;
            //发现是扩展对象,需要反射创建实例并提前注入到实例中
            case -1:
                Object instance = field.getType().newInstance();
                field.set(obj, instance);
                //递归处理扩展类的属性
                setFields(instance, messageUnpacker);
                break;
            //其他情况
            default:
        }
    }

    /**
     * 从buffer中读取list值
     *
     * @param list            已经完成初始化的list
     * @param messageUnpacker buffer
     * @throws Exception 异常
     */
    private void setListValue(List<Object> list, MessageUnpacker messageUnpacker) throws Exception {

        //拿到集合长度
        int length = messageUnpacker.unpackArrayHeader();
        //拿到数据类型
        int type = messageUnpacker.unpackInt();
        //拿到集合对象类型
        String classFullName = null;
        if (type == -1) {
            classFullName = messageUnpacker.unpackString();
        }
        //把数据加入到集合中
        if (length > 0) {
            switch (type) {
                case 1:
                    for (int i = 0; i < length; i++) {
                        list.add(messageUnpacker.unpackBoolean());
                    }
                    break;
                case 2:
                    for (int i = 0; i < length; i++) {
                        list.add(messageUnpacker.unpackByte());
                    }
                    break;
                case 3:
                    for (int i = 0; i < length; i++) {
                        list.add(messageUnpacker.unpackShort());
                    }
                    break;
                case 4:
                    for (int i = 0; i < length; i++) {
                        list.add(messageUnpacker.unpackInt());
                    }
                    break;
                case 5:
                    for (int i = 0; i < length; i++) {
                        list.add(messageUnpacker.unpackString());
                    }
                    break;
                case 6:
                    for (int i = 0; i < length; i++) {
                        list.add(messageUnpacker.unpackLong());
                    }
                    break;
                case 7:
                    for (int i = 0; i < length; i++) {
                        list.add(messageUnpacker.unpackDouble());
                    }
                    break;
                case 11:
                    for (int i = 0; i < length; i++) {
                        list.add(messageUnpacker.unpackFloat());
                    }
                    break;
                //说明这个对象是扩展对象递归处理
                case -1:
                    for (int i = 0; i < length; i++) {
                        Object instance = Class.forName(classFullName).newInstance();
                        list.add(instance);
                        setFields(instance, messageUnpacker);
                    }
                    break;
                //其他情况
                default:
            }
        }


    }


    /**
     * 设置对象所有的属性值
     *
     * @param obj                 对象实例
     * @param messageBufferPacker 编码器
     * @throws Exception 异常
     */
    private void putFields(Object obj, MessageBufferPacker messageBufferPacker) throws Exception {
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            //跳过指定字段的序列化
            if (field.getAnnotation(Ignore.class) != null) {
                continue;
            }
            //基本数据类型直接打包,扩展类型递归处理
            putValue(messageBufferPacker, field, obj);
        }
    }

    /**
     * 在buffer中添加值
     *
     * @param messageBufferPacker buffer
     * @param field               字段
     * @param obj                 对象实例
     * @throws Exception 异常
     */
    private void putValue(MessageBufferPacker messageBufferPacker, Field field, Object obj) throws Exception {
        Object fieldObj = field.get(obj);
        if (fieldObj == null) {
            //空值处理
            messageBufferPacker.packNil();
            return;
        }
        //基本类型声明的成员变量在初始化时会自动赋值！所以需要safeCast特殊处理
        switch (getIntegerValue(fieldObj.getClass())) {
            case 1:
                messageBufferPacker.packBoolean(safeCast(fieldObj,Boolean.class));
                break;
            case 2:
                messageBufferPacker.packByte(safeCast(fieldObj,Byte.class));
                break;
            case 3:
                messageBufferPacker.packShort(safeCast(fieldObj,Short.class));
                break;
            //整型
            case 4:
                messageBufferPacker.packInt(safeCast(fieldObj,Integer.class));
                break;
            case 5:
                messageBufferPacker.packString(cast(fieldObj));
                break;
            case 6:
                messageBufferPacker.packLong(safeCast(fieldObj,Long.class));
                break;
            case 7:
                messageBufferPacker.packDouble(safeCast(fieldObj,Double.class));
                break;
            case 11:
                messageBufferPacker.packFloat(safeCast(fieldObj,Float.class));
                break;
            //集合类型当作数组处理
            case 9:
                putArrayValue(fieldObj, messageBufferPacker);
                break;
            //说明这个对象是扩展对象递归处理
            case -1:
                putFields(fieldObj, messageBufferPacker);
                break;
            //其他情况
            default:
        }
    }

    /**
     * 处理集合/数组
     *
     * @param object              对象
     * @param messageBufferPacker buffer
     * @throws Exception 异常
     */
    private void putArrayValue(Object object, MessageBufferPacker messageBufferPacker) throws Exception {
        //遍历集合对象处理
        if (object instanceof List) {
            List<Object> list = cast(object);
            //根据约定,如果为空,这个方法不会执行
            assert list != null;
            messageBufferPacker.packArrayHeader(list.size());
            //遍历list集合,加入到buffer,因为集合需要保证类型一致性
            //所以通过第一个元素确定类型
            if (list.size() > 0) {
                Object listObj = list.get(0);
                int type = getIntegerValue(listObj.getClass());
                //把type拿出来记录存放的是什么类型的集合
                messageBufferPacker.packInt(type);
                //存放list里面的全类名,用于反射生成对象
                if (type == -1) {
                    messageBufferPacker.packString(listObj.getClass().getName());
                }
                basicListToBuffer(list, messageBufferPacker, type);
            } else {
                //占位符 保证集合类型后第二个字节是记录类型的（第一个字节是记录集合大小）
                messageBufferPacker.packInt(-2);
            }
        } else {
            //格式不对放个null
            messageBufferPacker.packNil();
        }

    }

    /**
     * 把list类型的数据全存入buffer中
     *
     * @param obj                 list集合
     * @param messageBufferPacker buffer
     * @param type                类型(基本类型/异常)
     * @throws Exception 异常
     */
    private void basicListToBuffer(List<Object> obj, MessageBufferPacker messageBufferPacker, int type) throws Exception {

        switch (type) {
            case 1:
                for (Object fieldObj : obj) {
                    messageBufferPacker.packBoolean(safeCast(fieldObj,Boolean.class));
                }
                break;
            case 2:
                for (Object fieldObj : obj) {
                    messageBufferPacker.packByte(safeCast(fieldObj,Byte.class));
                }
                break;
            case 3:
                for (Object fieldObj : obj) {
                    messageBufferPacker.packShort(safeCast(fieldObj,Short.class));
                }
                break;
            case 4:
                for (Object fieldObj : obj) {
                    messageBufferPacker.packInt(safeCast(fieldObj,Integer.class));
                }
                break;
            case 5:
                for (Object fieldObj : obj) {
                    messageBufferPacker.packString(cast(fieldObj));
                }
                break;
            case 6:
                for (Object fieldObj : obj) {
                    messageBufferPacker.packLong(safeCast(fieldObj,Long.class));
                }
                break;
            case 7:
                for (Object fieldObj : obj) {
                    messageBufferPacker.packDouble(safeCast(fieldObj,Double.class));
                }
                break;
            case 11:
                for (Object fieldObj : obj) {
                    messageBufferPacker.packFloat(safeCast(fieldObj,Float.class));
                }
                break;
            //禁止集合里面泛型还是集合
            //说明这个对象是扩展对象递归处理
            case -1:
                for (Object fieldObj : obj) {
                    putFields(fieldObj, messageBufferPacker);
                }
                break;
            //其他情况
            default:
        }
    }


    /**
     * 自动拆箱方法,String作为switch选择,主要是通过hash+equals(必要安全检查)实现的
     * 自己用hashmap+switch = hash+switch整数,整数switch是通过二分法查询
     *
     * @param clazz .class对象
     * @return 返回int整形, 如果为-1代表扩展类型
     */
    private int getIntegerValue(Class<?> clazz) {
        Integer typeValue = BASIC_TYPE_VALUES.get(clazz);
        return typeValue == null ? -1 : typeValue;
    }

    /**
     * 给buffer设置单一值
     * @param messageUnpacker 缓存
     * @param type 基本类型
     * @return 返回
     * @throws IOException 异常
     */
    public Object getBasicValue(MessageUnpacker messageUnpacker , int type) throws IOException {
        switch (type){
            case 1 : return messageUnpacker.unpackBoolean();
            case 2 : return messageUnpacker.unpackByte();
            case 3 : return messageUnpacker.unpackShort();
            case 4 : return messageUnpacker.unpackInt();
            case 5 : return messageUnpacker.unpackString();
            case 6 : return messageUnpacker.unpackLong();
            case 7 : return messageUnpacker.unpackDouble();
            case 11 : return messageUnpacker.unpackFloat();
            default:
        }
        return null;
    }

    /**
     *
     * @param messageUnpacker buffer
     * @param type type
     * @param size size
     * @param list list
     * @return return
     * @throws IOException error
     */
    public Object getBasicValue(MessageUnpacker messageUnpacker , int type , int size , List<Object> list) throws IOException {
        switch (type){
            case 1 : for(int i = 0;i < size ;i++){list.add(messageUnpacker.unpackBoolean());} break;
            case 2 : for(int i = 0;i < size ;i++){list.add(messageUnpacker.unpackByte());}break;
            case 3 : for(int i = 0;i < size ;i++){list.add(messageUnpacker.unpackShort());}break;
            case 4 : for(int i = 0;i < size ;i++){list.add(messageUnpacker.unpackInt());}break;
            case 5 : for(int i = 0;i < size ;i++){list.add(messageUnpacker.unpackString());}break;
            case 6 : for(int i = 0;i < size ;i++){list.add(messageUnpacker.unpackLong());}break;
            case 7 : for(int i = 0;i < size ;i++){list.add(messageUnpacker.unpackDouble());}break;
            case 11 :for(int i = 0;i < size ;i++){list.add(messageUnpacker.unpackFloat());}break;
            default:
        }
        return null;
    }

    /**
     *
     * @param messageBufferPacker buffer
     * @param type type
     * @param value value
     * @throws IOException error
     */

    public void putBasicValue(MessageBufferPacker messageBufferPacker , int type ,Object value) throws IOException {
        switch (type){
            case 1 : messageBufferPacker.packBoolean(safeCast(value,Boolean.class));break;
            case 2 : messageBufferPacker.packByte(safeCast(value,Byte.class));break;
            case 3 : messageBufferPacker.packShort(safeCast(value,Short.class));break;
            case 4 : messageBufferPacker.packInt(safeCast(value,Integer.class));break;
            case 5 : messageBufferPacker.packString(safeCast(value,String.class));break;
            case 6 : messageBufferPacker.packLong(safeCast(value,Long.class));break;
            case 7 : messageBufferPacker.packDouble(safeCast(value,Double.class));break;
            case 11 : messageBufferPacker.packFloat(safeCast(value,Float.class));break;
            default:
        }
    }

    /**
     *
     * @param messageBufferPacker buffer
     * @param type type
     * @param size size
     * @param list list
     * @throws IOException error
     */
    public void putBasicValue(MessageBufferPacker messageBufferPacker , int type , int size , List<Object> list) throws IOException {
        switch (type){
            case 1 : for(Object value : list){messageBufferPacker.packBoolean(safeCast(value,Boolean.class));}break;
            case 2 : for(Object value : list){messageBufferPacker.packByte(safeCast(value,Byte.class));}break;
            case 3 : for(Object value : list){messageBufferPacker.packShort(safeCast(value,Short.class));}break;
            case 4 : for(Object value : list){messageBufferPacker.packInt(safeCast(value,Integer.class));}break;
            case 5 : for(Object value : list){messageBufferPacker.packString(safeCast(value,String.class));}break;
            case 6 : for(Object value : list){messageBufferPacker.packLong(safeCast(value,Long.class));}break;
            case 7 : for(Object value : list){messageBufferPacker.packDouble(safeCast(value,Double.class));}break;
            case 11 : for(Object value : list){messageBufferPacker.packFloat(safeCast(value,Float.class));}break;
            default:
        }
    }

    /**
     * 约定如何存放类型信息,基本类型用
     * @param obj
     * @return
     */
    private String getTypeValue(Object obj){
        Class clazz = obj.getClass();
        int integerValue = getIntegerValue(clazz);
        if (integerValue != -1){
            return Integer.toString(integerValue);
        }
        return clazz.getName();
    }

    /**
     *
     * @param typeString
     * @return
     */
    private Class<?> getClassByString(String typeString){
        //先看看是不是基本类型
        if(!"-1".equals(typeString)){
            return BASIC_TYPE_CLASS.get(typeString);
        }
        try {
            return Class.forName(typeString);
        } catch (ClassNotFoundException e) {
            logger.error(e);
        }
        return null;
    }


    /**
     * 基本类型枚举类
     * 这块枚举类准备直接替换为基本类型
     */
    enum BasicTypeEnum {
        /**
         * 基本类型枚举对应数字
         */
        BOOLEAN("布尔类型", 1), BYTE("字节", 2), SHORT("短整型", 3),
        INTEGER("整型", 4), STRING("字符串", 5), LONG("长整形", 6),
        DOUBLE("双精度", 7), CHAR("字符", 8),FLOAT("浮点型",11),
        LIST("集合类型", 9), MAP("hash类型", 10);
        /**
         * 基本类型名称
         */
        String typeName;
        /**
         * 对应数字
         */
        int value;

        /**
         * 私有构造
         *
         * @param typeName 名称
         * @param value    值
         */
        BasicTypeEnum(String typeName, int value) {
            this.typeName = typeName;
            this.value = value;
        }
    }

    //map初始化
    static {
        //初始化Map
        //基本数据类型
        BASIC_TYPE_VALUES.put(Boolean.class, SerializerUtil.BasicTypeEnum.BOOLEAN.value);
        BASIC_TYPE_VALUES.put(boolean.class, SerializerUtil.BasicTypeEnum.BOOLEAN.value);
        BASIC_TYPE_VALUES.put(Byte.class, SerializerUtil.BasicTypeEnum.BYTE.value);
        BASIC_TYPE_VALUES.put(byte.class, SerializerUtil.BasicTypeEnum.BYTE.value);
        BASIC_TYPE_VALUES.put(Short.class, SerializerUtil.BasicTypeEnum.SHORT.value);
        BASIC_TYPE_VALUES.put(short.class, SerializerUtil.BasicTypeEnum.SHORT.value);
        BASIC_TYPE_VALUES.put(Integer.class, SerializerUtil.BasicTypeEnum.INTEGER.value);
        BASIC_TYPE_VALUES.put(int.class, SerializerUtil.BasicTypeEnum.INTEGER.value);
        BASIC_TYPE_VALUES.put(Long.class, SerializerUtil.BasicTypeEnum.LONG.value);
        BASIC_TYPE_VALUES.put(long.class, SerializerUtil.BasicTypeEnum.LONG.value);
        BASIC_TYPE_VALUES.put(Double.class, SerializerUtil.BasicTypeEnum.DOUBLE.value);
        BASIC_TYPE_VALUES.put(double.class, SerializerUtil.BasicTypeEnum.DOUBLE.value);
        BASIC_TYPE_VALUES.put(Float.class, SerializerUtil.BasicTypeEnum.FLOAT.value);
        BASIC_TYPE_VALUES.put(float.class, SerializerUtil.BasicTypeEnum.FLOAT.value);
        BASIC_TYPE_VALUES.put(Character.class, SerializerUtil.BasicTypeEnum.CHAR.value);
        BASIC_TYPE_VALUES.put(char.class, SerializerUtil.BasicTypeEnum.CHAR.value);
        //字符串
        BASIC_TYPE_VALUES.put(String.class, SerializerUtil.BasicTypeEnum.STRING.value);
        //集合类型
        BASIC_TYPE_VALUES.put(ArrayList.class, SerializerUtil.BasicTypeEnum.LIST.value);
        BASIC_TYPE_VALUES.put(List.class, SerializerUtil.BasicTypeEnum.LIST.value);
        BASIC_TYPE_VALUES.put(LinkedList.class, SerializerUtil.BasicTypeEnum.LIST.value);
        //map类型
        BASIC_TYPE_VALUES.put(HashMap.class, SerializerUtil.BasicTypeEnum.MAP.value);
        BASIC_TYPE_VALUES.put(Map.class, SerializerUtil.BasicTypeEnum.MAP.value);

        //初始化基本类型默认值
        BASIC_TYPE_INIT_VALUE.put(Boolean.class, false);
        BASIC_TYPE_INIT_VALUE.put(boolean.class, false);
        BASIC_TYPE_INIT_VALUE.put(Byte.class, 0);
        BASIC_TYPE_INIT_VALUE.put(byte.class, 0);
        BASIC_TYPE_INIT_VALUE.put(Short.class, 0);
        BASIC_TYPE_INIT_VALUE.put(short.class, 0);
        BASIC_TYPE_INIT_VALUE.put(Integer.class, 0);
        BASIC_TYPE_INIT_VALUE.put(int.class, 0);
        BASIC_TYPE_INIT_VALUE.put(Long.class, 0L);
        BASIC_TYPE_INIT_VALUE.put(long.class, 0L);
        BASIC_TYPE_INIT_VALUE.put(Double.class, 0);
        BASIC_TYPE_INIT_VALUE.put(double.class, 0);
        BASIC_TYPE_INIT_VALUE.put(Character.class, '0');
        BASIC_TYPE_INIT_VALUE.put(char.class, '0');
        //字符串
        BASIC_TYPE_INIT_VALUE.put(String.class, "");

        //统一返回包装类型
        BASIC_TYPE_CLASS.put("1" , Boolean.class);
        BASIC_TYPE_CLASS.put("2" , Byte.class);
        BASIC_TYPE_CLASS.put("3" , Short.class);
        BASIC_TYPE_CLASS.put("4" , Integer.class);
        BASIC_TYPE_CLASS.put("5" , String.class);
        BASIC_TYPE_CLASS.put("6" , Long.class);
        BASIC_TYPE_CLASS.put("7" , Double.class);
        BASIC_TYPE_CLASS.put("8" , Character.class);
        BASIC_TYPE_CLASS.put("9" , List.class);
        BASIC_TYPE_CLASS.put("11" , Boolean.class);
    }

    /**
     * 强制类型转化,消除警告
     *
     * @param <T> 转化对象
     * @return 返回对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object object) {
        try {
            return (T) object;
        } catch (ClassCastException e) {
            logger.error("类型转化出错！", e);
        }
        return null;
    }

    /**
     * 安全强制类型转化 主要针对基本类型
     * 初始化了基本类型Class对应初始化包装类的map
     * 因为包装类无法通过反射初始化对象
     * 实体中的基本类型 int / short 会在类初始化时赋初始值
     * 如果实体反序列化时当前属性为包装类,也能保证正常赋值
     *
     * @param obj 包装对象
     * @param clazz class
     * @param <T> 返回泛型
     * @return 返回
     */
    public static <T> T safeCast(Object obj, Class<T> clazz) {
        T retObject;
        try {
            retObject = clazz.cast(obj);
            //不返回空结果
            return retObject == null ? cast(BASIC_TYPE_INIT_VALUE.get(clazz)) : retObject;
        } catch (ClassCastException e) {
            // 异常情况
            return cast(BASIC_TYPE_INIT_VALUE.get(clazz));
        }
    }

}
