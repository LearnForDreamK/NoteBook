### 使用自实现高性能序列化工具，性能远超JDK,JSON,HESSIAN。

#### 一.准备

###### 1.pom

```xml
<dependencies>
        <!--fastJson 工具类-->
        <!-- https://mvnrepository.com/artifact/com.alibaba/fastjson -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.76</version>
        </dependency>
        <!--糊涂工具包-->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.6.5</version>
        </dependency>
        <!--lombok-->
        <!-- https://mvnrepository.com/artifact/org.projectlombok/lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.20</version>
            <scope>provided</scope>
        </dependency>

        <!--log4j日志包 -->
        <dependency>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
            <version>1.2.16</version>
        </dependency>
        <!--hessian序列化-->
        <!-- https://mvnrepository.com/artifact/com.caucho/hessian -->
        <dependency>
            <groupId>com.caucho</groupId>
            <artifactId>hessian</artifactId>
            <version>4.0.38</version>
        </dependency>
        <!--msgpack序列化-->
        <!-- https://mvnrepository.com/artifact/org.msgpack/msgpack-core -->
        <dependency>
            <groupId>org.msgpack</groupId>
            <artifactId>msgpack-core</artifactId>
            <version>0.8.22</version>
        </dependency>

    </dependencies>
```

###### 2.log4j

```properties
log4j.rootLogger=DEBUG,CONSOLE,A
log4j.addivity.org.apache=false

log4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender
log4j.appender.CONSOLE.Threshold=DEBUG
log4j.appender.CONSOLE.layout.ConversionPattern=%d{yyyy-MM-dd HH\:mm\:ss} -%-4r [%t] %-5p  %x - %m%n
log4j.appender.CONSOLE.Target=System.out
log4j.appender.CONSOLE.Encoding=UTF-8
log4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout

log4j.appender.A=org.apache.log4j.DailyRollingFileAppender
log4j.appender.A.Threshold=DEBUG
log4j.appender.A.File=${catalina.home}/logs/myapp.log
log4j.appender.A.DatePattern=yyyy-MM-dd'.log'
log4j.appender.A.layout.ConversionPattern=[FH_sys]  %d{yyyy-MM-dd HH\:mm\:ss} %5p %c{1}\:%L \: %m%n
log4j.appender.A.Encoding=UTF-8
log4j.appender.A.layout=org.apache.log4j.PatternLayout

```

###### 3.lombok

> idea必须要安装lombok插件。（删除测试方法后可以不导lombok包和插件）



#### 二.实现过程



##### 1.关于赋值（反射）

反序列化需要使用反射寻找所有的字段，然后反射赋值，赋值有两种方法，一种是反射获取set方法赋值,一种是反射暴力赋值，我刚开始没想到获取set方法赋值，然后使用反射暴力赋值，不过经过测试后，其实速度差距并不大，及其大量级的时候才能看出差距。

```java
public class testSpeed {
    public static void main(String[] args) throws Exception {
        Test t = new Test();
        Field[] names = t.getClass().getDeclaredFields();
        Field name = names[0];
        PropertyDescriptor pd = new PropertyDescriptor(name.getName(),t.getClass());
        Method setMethod = pd.getWriteMethod();
        StopWatch a = new StopWatch();
        StopWatch b = new StopWatch();
        a.start();
        for(int i = 0; i<1000000000 ; i++){
            name.setAccessible(true);
            name.set(t,"xx");
        }
        a.stop();
        b.start();
        for(int i = 0; i<1000000000 ; i++){
            //使用普通的set方法差距大概20倍，不过量级依旧很小
            //t.setName("xx");
            setMethod.invoke(t,"xx");
        }
        b.stop();
        System.out.println(a.getTotalTimeMillis());
        System.out.println(b.getTotalTimeMillis());
    }
}
//结果
4124
2345
```

海量数据差距并不大，所以我就没有修改了，有兴趣的话可以修改。

##### 2.关于hessian小细节

小细节让我了解框架设计者的厉害。

> https://github.com/msgpack/msgpack/blob/master/spec.md

首先了解一下数据存储方式，在字节流中，所有类型的数据都首先需要一个字节代表是什么类型的，但是有的数据比较独特，占用的字节比较少，比如布尔类型，null类型，0xc0就是表示是空值，0xc2、0xc3就是代表布尔类型的true/false，Int类型数据转换为字节流时，可能是1，2，3，4，9字节，使用官方文档的例子介绍，就是

```xml
int 8 stores a 8-bit signed integer
+--------+--------+
|  0xd0  |ZZZZZZZZ|
+--------+--------+

int 16 stores a 16-bit big-endian signed integer
+--------+--------+--------+
|  0xd1  |ZZZZZZZZ|ZZZZZZZZ|
+--------+--------+--------+

int 32 stores a 32-bit big-endian signed integer
+--------+--------+--------+--------+--------+
|  0xd2  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|
+--------+--------+--------+--------+--------+

int 64 stores a 64-bit big-endian signed integer
+--------+--------+--------+--------+--------+--------+--------+--------+--------+
|  0xd3  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|
+--------+--------+--------+--------+--------+--------+--------+--------+--------+
```

我在测试性能时，使用10000和100000数据的整形List集合测试序列化反序列化效率，在速度方面，我封装的msgpack速度还是远远领先于其他序列化方式，但让我吃惊的是，hessian序列化得到的字节数组大小和msgpack序列化的字节数组大小基本相同，每次固定差2范围以内的大小，hessian序列化是保存状态的，字段顺序都无所谓，是类似于map的key-value结构，差距应该很大的，所以肯定对基本类型使用了和msgpack相似的优化，我就很好奇这2的差距哪里来的，查看msgpack上面的文档描述后，我大概猜到了，每个数字都用一个字节保存类型，对应后面多少位保存数据大小，是一种极致的优化，然后2应该有多种可能，根据我自己的约定我序列化集合会用一个整形存储集合的大小，反序列化就可以让集合不用扩容，可能是这个字段导致大概2-3-4字节的误差。所以我猜测是因为hessian用固定大小的整形字节来表示集合大小，而我是用可变整形表示集合大小，所以会有+-4的误差。 

#### 三.完整代码

##### 1.主工具类

```java
package cc.hekun.serializer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.StopWatch;
import com.alibaba.fastjson.JSONObject;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.log4j.Logger;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 序列化工具类
 * @author ouyanghekun
 * @date 2021-05-10 09:23
 */
public class SerializerUtil {
    /**
     * 日志
     */
    private static final Logger logger = Logger.getLogger(SerializerUtil.class);
    /**
     * 存放基本类型class和数字的对应集合
     */
    private static final Map<Class<?>, Integer> BASIC_TYPE_VALUES = new HashMap<>();
    /**
     * 基本类型转化异常默认值
     */
    private static final Map<Class<?>, Object> BASIC_TYPE_INIT_VALUE = new HashMap<>();

    /**
     * JDK序列化对象,返回字节数组,对象必须实现serializable接口
     *
     * @param obj 序列化对象
     * @return 序列化字节数组
     */
    public static byte[] serializeByJdk(Object obj) {
        try (
                //try with resource 使用完自动关闭流
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                //FileOutputStream fileOutputStream = new FileOutputStream(outputFileUrl);
                ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            //写出对象
            outputStream.writeObject(obj);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            logger.error("jdk序列化出错", e);
        }
        return new byte[0];
    }

    /**
     * jdk反序列化对象,传入字节数组
     *
     * @param objectByteArray 需要反序列化的字节数组
     * @return 返回泛型转化的对象
     */
    public static <T> T unSerializeObjByJdk(byte[] objectByteArray) {
        try (
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(objectByteArray);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            //反序列化字节数组
            return cast(objectInputStream.readObject());
        } catch (Exception e) {
            logger.error("jdk反序列化异常", e);
        }
        //反序列出错返回空
        return null;
    }

    /**
     * json方式序列化对象,约定编码为UTF-8
     *
     * @param obj 序列化对象
     * @return 序列化字节数组
     */
    public static byte[] serializeByJson(Object obj) {
        try {
            String jsonString = JSONObject.toJSONString(obj);
            return jsonString.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("json序列化异常", e);
        }
        return null;
    }

    /**
     * json反序列化对象,传入字节数组
     *
     * @param objectByteArray 需要反序列化的字节数组
     * @return 返回泛型转化的对象
     */
    public static <T> T unSerializeByJson(byte[] objectByteArray, Class<T> clazz) {
        try {
            Object parse = JSONObject.parseObject(new String(objectByteArray, StandardCharsets.UTF_8), clazz);
            return cast(parse);
        } catch (Exception e) {
            logger.error("json反序列化异常", e);
        }
        return null;
    }

    /**
     * json反序列化对象,传入字节数组
     *
     * @param objectByteArray 需要反序列化的字节数组
     * @return 返回List集合对象
     */
    public static <T> List<T> unSerializeByJsonOnListType(byte[] objectByteArray, Class<T> clazz) {
        try {
            Object parse = JSONObject.parseArray(new String(objectByteArray, StandardCharsets.UTF_8), clazz);
            return cast(parse);
        } catch (Exception e) {
            logger.error("json反序列化异常", e);
        }
        return null;
    }

    /**
     * Hessian序列化对象,返回字节数组,对象必须实现serializable接口
     *
     * @param obj 序列化对象
     * @return 序列化字节数组
     */
    public static byte[] serializeByHessian(Object obj) {
        Hessian2Output outputStream = null;
        try (
            //try with resource 使用完自动关闭流
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            //初始化并写出对象
            outputStream = new Hessian2Output(byteArrayOutputStream);
            outputStream.writeObject(obj);
            //！！！！要刷新缓存,不然字节数组为空
            outputStream.completeMessage();
            outputStream.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            logger.error("hessian序列化出错", e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    logger.error("关闭hessian输出流出错", e);
                }
            }
        }
        return new byte[0];
    }

    /**
     * hessian反序列化对象,传入字节数组
     *
     * @param objectByteArray 需要反序列化的字节数组
     * @return 返回泛型转化的对象
     */
    public static <T> T unSerializeObjByHessian(byte[] objectByteArray) {
        Hessian2Input hessian2Input = null;
        try (
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(objectByteArray)) {
            hessian2Input = new Hessian2Input(byteArrayInputStream);
            //反序列化字节数组
            return cast(hessian2Input.readObject());
        } catch (Exception e) {
            logger.error("hessian反序列化异常", e);
        } finally {
            if (hessian2Input != null) {
                try {
                    hessian2Input.close();
                } catch (Exception e) {
                    logger.error("关闭hessian输入流出错", e);
                }
            }
        }
        //反序列出错返回空
        return null;
    }


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
     * @param obj 序列化对象
     * @return 字节数组
     */
    public static byte[] serializeByMsgPack(Object obj) {
        if(obj instanceof List){
            return serializeByMsgPackList(cast(obj));
        }
        MessageBufferPacker messageBufferPacker = null;
        byte[] res = null;
        //找出所有属性
        try {
            int type = getIntegerValue(obj.getClass());
            messageBufferPacker = MessagePack.newDefaultBufferPacker();
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

    /**
     * 序列化集合类型对象
     * @param listObj 集合对象
     * @return 返回序列化字节数组
     */
    private static byte[] serializeByMsgPackList(List<Object> listObj){
        MessageBufferPacker messageBufferPacker = null;
        byte[] res = null;
        //空列表直接返回空
        if(Objects.isNull(listObj)){
            return null;
        }
        //找出所有属性
        try {
            messageBufferPacker = MessagePack.newDefaultBufferPacker();
            //约定,第一个数字为集合长度
            int size = listObj.size();
            int type = -1;
            messageBufferPacker.packInt(size);
            //空集合
            if(size > 0){
                //设置类型
                type = getIntegerValue(listObj.get(0).getClass());
            }
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
     * 反序列化集合对象
     * @param bytes 字节数组
     * @param clazz 类型
     * @param <T> 泛型
     * @return 返回泛型结果
     */
    public static <T> List<T> unSerializeByMsgPackOnList(byte[] bytes, Class<T> clazz) {
        List<T> res = null;
        MessageUnpacker messageUnpacker = null;
        try {
            int type = getIntegerValue(clazz);
            messageUnpacker = MessagePack.newDefaultUnpacker(bytes);
            //通过约定拿到长度
            int size = messageUnpacker.unpackInt();
            res = new ArrayList<>(size);
            //批量解析对象
            if(type == -1){
                for(int i = 0 ; i < size ;i++){
                    T resObj = clazz.newInstance();
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
     * msgpack反序列化方法
     *
     * @param bytes 字节流
     * @param clazz class
     * @param <T>   自定义泛型
     * @return 自定义泛型对象
     */
    public static <T> T unSerializeByMsgPack(byte[] bytes, Class<T> clazz) {
        T resObj = null;
        MessageUnpacker messageUnpacker = null;
        try {
            int type = getIntegerValue(clazz);
            messageUnpacker = MessagePack.newDefaultUnpacker(bytes);
            //反射创建对象
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
     * 设置对象所有的属性值
     *
     * @param obj             对象实例
     * @param messageUnpacker 编码器
     * @throws Exception 异常
     */
    private static void setFields(Object obj, MessageUnpacker messageUnpacker) throws Exception {
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
    private static void setValue(MessageUnpacker messageUnpacker, Field field, Object obj) throws Exception {
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
    private static void setListValue(List<Object> list, MessageUnpacker messageUnpacker) throws Exception {

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
    private static void putFields(Object obj, MessageBufferPacker messageBufferPacker) throws Exception {
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
    private static void putValue(MessageBufferPacker messageBufferPacker, Field field, Object obj) throws Exception {
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
    private static void putArrayValue(Object object, MessageBufferPacker messageBufferPacker) throws Exception {
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
    private static void basicListToBuffer(List<Object> obj, MessageBufferPacker messageBufferPacker, int type) throws Exception {

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
    private static int getIntegerValue(Class<?> clazz) {
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
    public static Object getBasicValue(MessageUnpacker messageUnpacker , int type) throws IOException {
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
    public static Object getBasicValue(MessageUnpacker messageUnpacker , int type , int size , List<Object> list) throws IOException {
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

    public static void putBasicValue(MessageBufferPacker messageBufferPacker , int type ,Object value) throws IOException {
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
    public static void putBasicValue(MessageBufferPacker messageBufferPacker , int type , int size , List<Object> list) throws IOException {
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
     * 基本类型枚举类
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
        BASIC_TYPE_VALUES.put(Boolean.class, BasicTypeEnum.BOOLEAN.value);
        BASIC_TYPE_VALUES.put(boolean.class, BasicTypeEnum.BOOLEAN.value);
        BASIC_TYPE_VALUES.put(Byte.class, BasicTypeEnum.BYTE.value);
        BASIC_TYPE_VALUES.put(byte.class, BasicTypeEnum.BYTE.value);
        BASIC_TYPE_VALUES.put(Short.class, BasicTypeEnum.SHORT.value);
        BASIC_TYPE_VALUES.put(short.class, BasicTypeEnum.SHORT.value);
        BASIC_TYPE_VALUES.put(Integer.class, BasicTypeEnum.INTEGER.value);
        BASIC_TYPE_VALUES.put(int.class, BasicTypeEnum.INTEGER.value);
        BASIC_TYPE_VALUES.put(Long.class, BasicTypeEnum.LONG.value);
        BASIC_TYPE_VALUES.put(long.class, BasicTypeEnum.LONG.value);
        BASIC_TYPE_VALUES.put(Double.class, BasicTypeEnum.DOUBLE.value);
        BASIC_TYPE_VALUES.put(double.class, BasicTypeEnum.DOUBLE.value);
        BASIC_TYPE_VALUES.put(Float.class, BasicTypeEnum.FLOAT.value);
        BASIC_TYPE_VALUES.put(float.class, BasicTypeEnum.FLOAT.value);
        BASIC_TYPE_VALUES.put(Character.class, BasicTypeEnum.CHAR.value);
        BASIC_TYPE_VALUES.put(char.class, BasicTypeEnum.CHAR.value);
        //字符串
        BASIC_TYPE_VALUES.put(String.class, BasicTypeEnum.STRING.value);
        //集合类型
        BASIC_TYPE_VALUES.put(ArrayList.class, BasicTypeEnum.LIST.value);
        BASIC_TYPE_VALUES.put(List.class, BasicTypeEnum.LIST.value);
        BASIC_TYPE_VALUES.put(LinkedList.class, BasicTypeEnum.LIST.value);
        //map类型
        BASIC_TYPE_VALUES.put(HashMap.class, BasicTypeEnum.MAP.value);
        BASIC_TYPE_VALUES.put(Map.class, BasicTypeEnum.MAP.value);

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
    }


    /**
     * main方法
     *
     * @param args --------------
     */
    public static void main(String[] args) {
        //speedTest();
        //testMethod();
        speedTestOnList();
    }

    /**
     * 测试序列化方法
     */
    public static void testMethod() {

        TestData testData = new TestData("testString", 666, DateUtil.date());
        TestData testData2 = new TestData("testString222", 22, DateUtil.date());
        List<TestData> listData = new ArrayList<>();
        listData.add(testData);
        listData.add(testData2);

        logger.info("--------------JDK序列化测试----------------------");
        byte[] arr = serializeByJdk(testData);
        TestData res = unSerializeObjByJdk(arr);
        logger.info("普通对象结果:" + res);
        byte[] arrList = serializeByJdk(listData);
        List<TestData> resList = unSerializeObjByJdk(arrList);
        logger.info("list对象结果:" + resList);

        logger.info("--------------Json序列化测试----------------------");
        byte[] arr2 = serializeByJson(testData);
        TestData res2 = unSerializeByJson(arr2, TestData.class);
        logger.info("结果:" + res2);
        byte[] arr2List = serializeByJson(listData);
        List<TestData> res2List = unSerializeByJsonOnListType(arr2List, TestData.class);
        logger.info("list对象结果:" + res2List);

        logger.info("--------------hessian序列化测试----------------------");
        byte[] arr3 = serializeByHessian(testData);
        TestData res3 = unSerializeObjByHessian(arr3);
        logger.info("普通对象结果:" + res3);
        byte[] arr3List = serializeByHessian(listData);
        List<TestData> resList3 = unSerializeObjByHessian(arr3List);
        logger.info("list对象结果:" + resList3);

        logger.info("--------------msgpack序列化测试----------------------");
        byte[] arr4 = serializeByMsgPack(testData);
        TestData res4 = unSerializeByMsgPack(arr4,TestData.class);
        logger.info("普通对象结果:" + res4);

    }

    /**
     * 普通对象时间测试
     */
    public static void speedTest(){
        int watchNum = 4 , msgSize = 2000;
        StopWatch[] stopWatch = new StopWatch[4];
        for(int i = 0 ; i < watchNum ; i++){
            stopWatch[i] = new StopWatch();
        }
        List<TestData> testDates = new ArrayList<>();
        for(int i = 0 ; i < msgSize ; i++ ){
            TestData oneData = new TestData("testString", null,DateUtil.date());
            testDates.add(oneData);
        }
        InnerTestData innerTestData = new InnerTestData();
        innerTestData.setAllTestData(testDates);

        logger.info("msgpack速度-------------------------->");
        stopWatch[0].start();
        byte[] arr = serializeByMsgPack(innerTestData);
        unSerializeByMsgPack(arr, InnerTestData.class);
        stopWatch[0].stop();
        logger.info("msgpack时间 : {"+stopWatch[0].getTotalTimeMillis()+"} ms");
        logger.info("字节数组大小: "+arr.length);


        logger.info("jdk速度-------------------------->");
        stopWatch[1].start();
        byte[] arr1 = serializeByJdk(innerTestData);
        unSerializeObjByJdk(arr1);
        stopWatch[1].stop();
        logger.info("jdk时间 : {"+stopWatch[1].getTotalTimeMillis()+"} ms");
        logger.info("字节数组大小: "+arr1.length);


        logger.info("json速度-------------------------->");
        stopWatch[2].start();
        byte[] arr2 = serializeByJson(innerTestData);
        unSerializeByJson(arr2,InnerTestData.class);
        stopWatch[2].stop();
        logger.info("json时间 : {"+stopWatch[2].getTotalTimeMillis()+"} ms");
        assert arr2 != null;
        logger.info("字节数组大小: "+arr2.length);


        logger.info("hessian速度-------------------------->");
        stopWatch[3].start();
        byte[] arr3 = serializeByHessian(innerTestData);
        unSerializeObjByHessian(arr3);
        stopWatch[3].stop();
        logger.info("hessian时间 : {"+stopWatch[3].getTotalTimeMillis()+"} ms");
        logger.info("字节数组大小: "+arr3.length);


    }

    /**
     * List对象时间测试
     */
    public static void speedTestOnList(){
        int size = 10000 , watchNum = 4;
        StopWatch[] stopWatch = new StopWatch[4];
        for(int i = 0 ; i < watchNum ; i++){
            stopWatch[i] = new StopWatch();
        }
        //数字集合
        List<Integer> arrayList= new ArrayList<>();
        Random random = new Random();
        for(int i = 0 ; i < size ; i++){
            arrayList.add(random.nextInt());
        }
        //对象集合
        List<TestData> testData =  new ArrayList<>();
        for (int i = 0; i < size; i++) {
            testData.add(new TestData("name1",555,DateUtil.date()));
        }
        //计算速度开始
        logger.info("jdk序列化测试");
        stopWatch[0].start();
        byte[] a = serializeByJdk(arrayList);
        unSerializeObjByJdk(a);
        stopWatch[0].stop();
        logger.info("jdk时间------>"+stopWatch[0].getTotalTimeMillis()+"ms");
        logger.info("jdk大小: "+a.length);

        logger.info("json序列化测试");
        stopWatch[1].start();
        byte[] b = serializeByJson(arrayList);
        unSerializeByJsonOnListType(b,Integer.class);
        stopWatch[1].stop();
        logger.info("json时间------>"+stopWatch[1].getTotalTimeMillis()+"ms");
        logger.info("json大小: "+b.length);


        logger.info("hessian序列化测试");
        stopWatch[2].start();
        byte[] c = serializeByHessian(arrayList);
        unSerializeObjByHessian(c);
        stopWatch[2].stop();
        logger.info("hessian时间------>"+stopWatch[2].getTotalTimeMillis()+"ms");
        logger.info("hessian大小: "+c.length);


        logger.info("msgpack序列化测试");
        stopWatch[3].start();
        byte[] d = serializeByMsgPack(arrayList);
        unSerializeByMsgPackOnList(d,Integer.class);
        stopWatch[3].stop();
        logger.info("msgpack时间------>"+stopWatch[3].getTotalTimeMillis()+"ms");
        logger.info("msgpack大小: "+d.length);
    }

    /**
     * 测试的实体类
     */
    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestData implements Serializable {
        /**
         * 名称
         */
        private String dataName;
        /**
         * id
         */
        private Integer dataId;

        /**
         * 创建时间
         */
        @Ignore
        private Date createTime;
    }

    /**
     * 测试的实体类
     */
    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    static class InnerTestData implements Serializable {
        /**
         *
         */
        private String name;
        /**
         *
         */
        private String uid;
        /**
         *
         */
        private int age;
        /**
         *
         */
        private TestData testData;
        /**
         *
         */
        private List<Integer> allId;

        /**
         *
         */
        private List<TestData> allTestData;

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
```

##### 2.附加注解

```java
package cc.hekun.serializer;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * msgpack表示不序列化某字段
 * @author ouyanghekun
 * @date 2021-05-11 17:58
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Ignore {
}

```

#### 四.后续

##### 1.问题

1）线上警告
安全系统发警告告诉发现有反序列化漏洞，吓得我赶紧找，然后文档说是我工具类里附带的JDK的反序列化漏洞，不过我的工具主要是给内部缓存使用的，只要缓存不被黑应该不会有问题owo。

##### 2.扩展

1）计划引入函数式接口+责任链模式实现自定义扩展类解析。switch中的default可以用于处理扩展类。

2）有时间加入map的支持。

3）参考用例，官方github最新文档。

> https://github.com/msgpack/msgpack-java
>
> https://github.com/msgpack/msgpack-java/blob/develop/msgpack-core/src/test/java/org/msgpack/core/example/MessagePackExample.java
>
> https://msgpack.org/

4）发现官方用例有其他方式更高效率的解析，有时间会更新。

5）和Hessian对比