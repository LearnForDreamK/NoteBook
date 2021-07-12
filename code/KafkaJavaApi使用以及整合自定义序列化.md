

# Kafka-JavaApi使用+整合自定义序列化



## 一.集群环境配置

### 1.创建主题

之前已经分别搭建了ZK集群和KAFKA集群（3个broker），所以直接创建topic

> 分配规则：https://blog.csdn.net/lanmolei814/article/details/78353898

```shell
#
kafka-topics.sh
--create //操作类型
--zookeeper localhost:2181 //kafka依赖的zookeeper地址
--replication-factor  2//副本因子
--partitions 1 //分区数量
--topic test //topic 名称

#我先创建一个单分区单副本的
kafka-topics.sh --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic hekunSingleTopic
```

创建结果

```shell
[root@VM-12-15-centos ~]# kafka-topics.sh --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic hekunSingleTopic
OpenJDK 64-Bit Server VM warning: If the number of processors is expected to increase from one, then you should configure the number of parallel GC threads appropriately using -XX:ParallelGCThreads=N
Created topic hekunSingleTopic.
```

相关命令

```shell
#查看TOPIC
kafka-topics.sh --zookeeper localhost:2181 --list
#查看详情
kafka-topics.sh --zookeeper localhost:2181 --describe --topic hekunSingleTopic
#删除
kafka-topics.sh --zookeeper localhost:2181 --delete --topic hekunSingleTopic
```

#### 生产者消费者

命令行的方式启动

```shell
#生产者
kafka-console-producer.sh --broker-list localhost:9092 --topic hekunSingleTopic
#新版消费者配置,老版本使用--zookeeper xxxx:2181 进行消费
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic hekunSingleTopic --from-beginning
```

> 生产者消费者：https://blog.csdn.net/dhaibo1986/article/details/106996547/



### 2.本地代码

```java
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
```

### 3.依赖

```xml
        <!-- https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients -->
        <dependency>
            <groupId>org.apache.kafka</groupId>
            <artifactId>kafka-clients</artifactId>
            <version>2.1.1</version>
        </dependency>
```

## 二.自定义序列化

```java
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
```

SpringBoot在application中配置即可，使用客户端就在Properties中设置key/value序列化方式。

