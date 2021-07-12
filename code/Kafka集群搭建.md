[TOC]

# Kalfk集群搭建



## 一.安装配置

### 1.下载

首先下载kafka。

```shell
# 下载
wget https://www-eu.apache.org/dist/kafka/2.2.0/kafka_2.12-2.2.0.tgz
# 解压,2.12是scala版本号，2.2.0是kafka版本号
tar -xzf kafka_2.12-2.2.0.tgz
```



```shell
[root@VM-12-15-centos apps]# wget https://www-eu.apache.org/dist/kafka/2.2.0/kafka_2.12-2.2.0.tgz
--2021-06-15 20:01:47--  https://www-eu.apache.org/dist/kafka/2.2.0/kafka_2.12-2.2.0.tgz
Resolving www-eu.apache.org (www-eu.apache.org)... 95.216.26.30, 2a01:4f9:2a:1a61::2
Connecting to www-eu.apache.org (www-eu.apache.org)|95.216.26.30|:443... connected.
HTTP request sent, awaiting response... 302 Found
Location: https://downloads.apache.org/kafka/2.2.0/kafka_2.12-2.2.0.tgz [following]
--2021-06-15 20:01:48--  https://downloads.apache.org/kafka/2.2.0/kafka_2.12-2.2.0.tgz
Resolving downloads.apache.org (downloads.apache.org)... 135.181.209.10, 88.99.95.219, 135.181.214.104, ...
Connecting to downloads.apache.org (downloads.apache.org)|135.181.209.10|:443... connected.
HTTP request sent, awaiting response... 404 Not Found
2021-06-15 20:01:49 ERROR 404: Not Found.

```

如上，我用这种方式下不下来这个版本，为了保持版本一致，我去官网下载了相同版本，用XFTP传到相同目录。

> https://kafka.apache.org/downloads

上传解压后，配置环境变量

```shell
#配置环境变量
export KAFKA_HOME=/apps/kafka_2.12-2.2.0
export PATH=$PATH:$JAVA_HOME/bin:$KAFKA_HOME/bin
```

### 2.启动

#### 1）命令

配置好环境变量后，使用命令启动集群。

```shell
kafka-server-start.sh $KAFKA_HOME/config/server.properties
```

#### 2）异常解决

##### 1）开放端口

启动后出现了异常

```shell
org.apache.kafka.common.KafkaException: Socket server failed to bind to xxx.xxx.xxx.xxx:9092: Cannot assign requested address.
```

然后我怀疑是端口未开放的问题，上云管理台开放了TCP端口，发现还是一样的错误，然后开放UDP端口，依旧如此，网上查询也没有相同的情况，最后我看到配置文件中的注释

```shell
#   EXAMPLE:
#   listeners = PLAINTEXT://your.host.name:9092
```

##### 2）域名配置

hostname难道不能直接用ip？ 然后我自己修改/etc/hosts 建立了域名和ip的映射，然后修改配置文件中的listeners上对应的ip为域名。启动，发现出现ZK超时信息

```shell
kafka.zookeeper.ZooKeeperClientTimeoutException: Timed out waiting for connection while in state: CONNECTING
```

我一看这超时，赶紧去配置文件中修改ZK集群的ip为域名，然后在hosts里面修改，然后启动，发现出现开始的错误。

```shell
org.apache.kafka.common.KafkaException: Socket server failed to bind to hekunskafka:9092: Cannot assign requested address.
```

##### 3）配置修改

到这人都裂开了，百度全是一样的答案，然后去谷歌了一下问题，最终发现应该是配置的问题，配置文件中配置项应该这样：

```shell
#原来的listeners=PLAINTEXT://ip:9092
# 调整监听地址配置
advertised.listeners=PLAINTEXT://ip:9092
```

然后启动，终于启动成功！。

> 绑定错误解决：https://www.jianshu.com/p/b3841de241af

#### 3）其他

```shell
#启动
kafka-server-start.sh $KAFKA_HOME/config/server.properties
# 后台启动
kafka-server-start.sh -daemon $KAFKA_HOME/config/server.properties
#停止Kafka
kafka-server-stop.sh stop
# 查看topic
kafka-topics.sh --bootstrap-server node01:9092,node02:9092,node03:9092 --list
```

#### 4）ZK查看

看一下ZK节点

```shell
sh zkCli.sh -server localhost:2181
ls /
[admin, brokers, cluster, config, consumers, controller, controller_epoch, isr_change_notification, latest_producer_id_block, log_dir_event_notification, xixikun, zookeeper]

#查看brokers数量
ls /brokers/ids
#我这里是
[0,1,2]
```

这里看起来很乱，所以在配置文件里配置ZK时，可以在端口后面加/kafka供维护方便，我忘了加了。

### 3.最终配置文件

```properties
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# see kafka.server.KafkaConfig for additional details and defaults

############################# Server Basics #############################

# The id of the broker. This must be set to a unique integer for each broker.
#集群中每个节点唯一标识
broker.id=0

############################# Socket Server Settings #############################

# The address the socket server listens on. It will get the value returned from 
# java.net.InetAddress.getCanonicalHostName() if not configured.
#   FORMAT:
#     listeners = listener_name://host_name:port
#   EXAMPLE:
#     listeners = PLAINTEXT://your.host.name:9092
#监听地址
advertised.listeners=PLAINTEXT://175.24.249.111:9092

# Hostname and port the broker will advertise to producers and consumers. If not set, 
# it uses the value for "listeners" if configured.  Otherwise, it will use the value
# returned from java.net.InetAddress.getCanonicalHostName().
#advertised.listeners=PLAINTEXT://your.host.name:9092

# Maps listener names to security protocols, the default is for them to be the same. See the config documentation for more details
#listener.security.protocol.map=PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL

# The number of threads that the server uses for receiving requests from the network and sending responses to the network
num.network.threads=3

# The number of threads that the server uses for processing requests, which may include disk I/O
num.io.threads=8

# The send buffer (SO_SNDBUF) used by the socket server
socket.send.buffer.bytes=102400

# The receive buffer (SO_RCVBUF) used by the socket server
socket.receive.buffer.bytes=102400

# The maximum size of a request that the socket server will accept (protection against OOM)
socket.request.max.bytes=104857600


############################# Log Basics #############################

# A comma separated list of directories under which to store log files
#kafka数据分片存放数据的位置
log.dirs=/apps/apppid/kafkaData

# The default number of log partitions per topic. More partitions allow greater
# parallelism for consumption, but this will also result in more files across
# the brokers.
num.partitions=1

# The number of threads per data directory to be used for log recovery at startup and flushing at shutdown.
# This value is recommended to be increased for installations with data dirs located in RAID array.
num.recovery.threads.per.data.dir=1

############################# Internal Topic Settings  #############################
# The replication factor for the group metadata internal topics "__consumer_offsets" and "__transaction_state"
# For anything other than development testing, a value greater than 1 is recommended for to ensure availability such as 3.
offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1

############################# Log Flush Policy #############################

# Messages are immediately written to the filesystem but by default we only fsync() to sync
# the OS cache lazily. The following configurations control the flush of data to disk.
# There are a few important trade-offs here:
#    1. Durability: Unflushed data may be lost if you are not using replication.
#    2. Latency: Very large flush intervals may lead to latency spikes when the flush does occur as there will be a lot of data to flush.
#    3. Throughput: The flush is generally the most expensive operation, and a small flush interval may lead to excessive seeks.
# The settings below allow one to configure the flush policy to flush data after a period of time or
# every N messages (or both). This can be done globally and overridden on a per-topic basis.

# The number of messages to accept before forcing a flush of data to disk
#log.flush.interval.messages=10000

# The maximum amount of time a message can sit in a log before we force a flush
#log.flush.interval.ms=1000

############################# Log Retention Policy #############################

# The following configurations control the disposal of log segments. The policy can
# be set to delete segments after a period of time, or after a given size has accumulated.
# A segment will be deleted whenever *either* of these criteria are met. Deletion always happens
# from the end of the log.

# The minimum age of a log file to be eligible for deletion due to age
log.retention.hours=168

# A size-based retention policy for logs. Segments are pruned from the log unless the remaining
# segments drop below log.retention.bytes. Functions independently of log.retention.hours.
#log.retention.bytes=1073741824

# The maximum size of a log segment file. When this size is reached a new log segment will be created.
log.segment.bytes=1073741824

# The interval at which log segments are checked to see if they can be deleted according
# to the retention policies
log.retention.check.interval.ms=300000

############################# Zookeeper #############################

# Zookeeper connection string (see zookeeper docs for details).
# This is a comma separated host:port pairs, each corresponding to a zk
# server. e.g. "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002".
# You can also append an optional chroot string to the urls to specify the
# root directory for all kafka znodes.
#配置ZK集群节点
zookeeper.connect=zk24:2181,azk12:2181,tzk12:2181

# Timeout in ms for connecting to zookeeper
zookeeper.connection.timeout.ms=6000


############################# Group Coordinator Settings #############################

# The following configuration specifies the time, in milliseconds, that the GroupCoordinator will delay the initial consumer rebalance.
# The rebalance will be further delayed by the value of group.initial.rebalance.delay.ms as new members join the group, up to a maximum of max.poll.interval.ms.
# The default value for this is 3 seconds.
# We override this to 0 here as it makes for a better out-of-the-box experience for development and testing.
# However, in production environments the default value of 3 seconds is more suitable as this will help to avoid unnecessary, and potentially expensive, rebalances during application startup.
group.initial.rebalance.delay.ms=0
```

### 4.其他节点

其他节点只需要修改broker.id和listener即可，我配置了三台服务器。



### 5.引用

> https://juejin.cn/post/6960867173916999693