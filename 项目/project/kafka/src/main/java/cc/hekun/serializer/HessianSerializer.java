package cc.hekun.serializer;

import cc.hekun.action.SerializerMethod;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * hessian序列化的对象必须实现Serializable接口
 * @author ouyanghekun
 * @date 2021-06-04 14:51
 */
public class HessianSerializer extends SerializerMethod {

    /**
     * 日志
     */
    private static final Logger logger = Logger.getLogger(HessianSerializer.class);

    @Override
    public byte[] doSerialize(Object obj) {
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

    @Override
    public Object deSerialize(byte[] objectByteArray, Class<?> clazz) {
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
