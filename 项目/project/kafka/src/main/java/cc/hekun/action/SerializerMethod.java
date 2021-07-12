package cc.hekun.action;

import java.util.List;

/**
 * 序列化接口,可以通过实现这个接口导入自己的序列化方式
 * 比如针对某些对象使用jdk的,某些类型使用hessian的，可以自己定制
 * @author ouyanghekun
 * @date 2021-06-04 14:11
 */
public abstract class SerializerMethod {

    /**
     * 序列化
     * @param obj 序列化对象
     * @return 字节数组
     */
    public  byte[] doSerialize(Object obj){
        return null;
    }

    /**
     * 反序列化
     * @param objectByteArray 字节数组
     * @param clazz 反序列化类型
     * @return 泛型对象
     */
    public Object deSerialize(byte[] objectByteArray , Class<?> clazz){
        return null;
    }

    /**
     * 反序列化
     * @param objectByteArray 字节数组
     * @param clazz 类别
     * @return 集合对象
     */
    public Object deSerializeWithListType(byte[] objectByteArray , Class<?> clazz){
        return null;
    }

    /**
     * 反序列化
     * @param objectByteArray
     * @return
     */
    public Object deSerialize(byte[] objectByteArray){
        return null;
    }

}
