package cc.hekun.serializer;

import cc.hekun.action.SerializerMethod;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;

import java.nio.charset.StandardCharsets;

/**
 * 约定以UTF-8把json字符串转化为字节数组
 * @author ouyanghekun
 * @date 2021-06-04 14:37
 */
public class JsonSerializer extends SerializerMethod {

    /**
     * 日志
     */
    private static final Logger logger = Logger.getLogger(JsonSerializer.class);

    @Override
    public byte[] doSerialize(Object obj) {
        try {
            String jsonString = JSONObject.toJSONString(obj);
            return jsonString.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("json序列化异常", e);
        }
        return null;
    }

    @Override
    public Object deSerialize(byte[] objectByteArray, Class<?> clazz) {
        try {
            Object parse = JSONObject.parseObject(new String(objectByteArray, StandardCharsets.UTF_8), clazz);
            return cast(parse);
        } catch (Exception e) {
            logger.error("json反序列化异常", e);
        }
        return null;
    }

    @Override
    public Object deSerializeWithListType(byte[] objectByteArray, Class<?> clazz) {
        try {
            Object parse = JSONObject.parseArray(new String(objectByteArray, StandardCharsets.UTF_8), clazz);
            return cast(parse);
        } catch (Exception e) {
            logger.error("json反序列化异常", e);
        }
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
