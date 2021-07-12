package cc.hekun;


import cc.hekun.enums.DefaultSerializerEnum;
import cc.hekun.serializer.SerializerUtil;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class Main {


    public static void main(String[] args) {

        Properties props = new Properties();
        // Kafka服务端的主机名和端口号
        props.put("bootstrap.servers", "175.24.249.111:9092");
        // 等待所有副本节点的应答
        props.put("acks", "all");
        // 消息发送最大尝试次数
        props.put("retries", 0);
        // 一批消息处理大小
        props.put("batch.size", 16384);
        // 增加服务端请求延时
        props.put("linger.ms", 1);
        // 发送缓存区内存大小
        props.put("buffer.memory", 33554432);
        // key序列化
        props.put("key.serializer", "cc.hekun.impl.MessagepackImpl");
        // value序列化
        props.put("value.serializer", "cc.hekun.impl.MessagepackImpl");
        // 自定义分区（com.lzhpo.kafka.custompartitionProducer.CustomPartitioner）
        //props.put("partitioner.class", "com.lzhpo.kafka.custompartitionProducer.CustomPartitioner");

        Producer<String, String> producer = new KafkaProducer<>(props);
        producer.send(new ProducerRecord<String, String>("hekunSingleTopic", "", "messageIscomming"));
        producer.close();

    }


}
