package cc.hekun.impl;

import cc.hekun.enums.DefaultSerializerEnum;
import lombok.Builder;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class MessagepackDeImpl<T> implements Deserializer<T> {
    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public T deserialize(String s, byte[] bytes) {
        return (T)DefaultSerializerEnum.MSG_PACK.getSerializerMethod().deSerialize(bytes);
    }

    @Override
    public T deserialize(String topic, Headers headers, byte[] data) {
        return Deserializer.super.deserialize(topic, headers, data);
    }

    @Override
    public void close() {

    }
}
