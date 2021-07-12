package cc.hekun.serializer;

import cc.hekun.action.SerializerMethod;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * JDK支持任何类型的序列化,但是效率低,转化的字节数组占用空间大,兼容性差
 * @author ouyanghekun
 * @date 2021-06-04 14:30
 */
public class JdkSerializer extends SerializerMethod {
    /**
     * 日志
     */
    private static final Logger logger = Logger.getLogger(JdkSerializer.class);

    @Override
    public byte[] doSerialize(Object obj) {
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

    @Override
    public Object deSerialize(byte[] objectByteArray, Class<?> clazz) {
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
}
