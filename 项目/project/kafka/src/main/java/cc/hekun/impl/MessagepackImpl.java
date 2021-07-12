package cc.hekun.impl;

import cc.hekun.SerializeTask;
import cc.hekun.enums.DefaultSerializerEnum;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MessagepackImpl<T> implements Serializer<T>{

    private static final Logger logger = Logger.getLogger(SerializeTask.class);
    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }
    @Override
    public byte[] serialize(String s, T t) {
        logger.info("序列化开始,序列化对象为: "+t);
        byte[] res = DefaultSerializerEnum.MSG_PACK.getSerializerMethod().doSerialize(t);
        return res;
    }

    @Override
    public byte[] serialize(String topic, Headers headers, T data) {
        return Serializer.super.serialize(topic, headers, data);
    }

    @Override
    public void close() {
        logger.info("序列化完成");
    }
}
