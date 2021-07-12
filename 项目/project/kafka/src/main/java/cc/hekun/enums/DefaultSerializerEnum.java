package cc.hekun.enums;

import cc.hekun.action.SerializerMethod;
import cc.hekun.serializer.HessianSerializer;
import cc.hekun.serializer.JdkSerializer;
import cc.hekun.serializer.JsonSerializer;
import cc.hekun.serializer.MessagePackSerializer;

/**
 * 序列化枚举类
 * @author ouyanghekun
 * @date 2021-06-04 17:30
 */

public enum DefaultSerializerEnum {
    /**
     * 提供的基础序列化方法
     */
    JDK(new JdkSerializer()) , JSON(new JsonSerializer()) ,
    HESSIAN(new HessianSerializer()) , MSG_PACK(new MessagePackSerializer());

    /**
     *
     */
    private SerializerMethod serializerMethod;

    /**
     * 枚举类
     * @param serializerMethod
     */
    private DefaultSerializerEnum(SerializerMethod serializerMethod){
        this.serializerMethod = serializerMethod;
    }

    public SerializerMethod getSerializerMethod() {
        return serializerMethod;
    }
}
