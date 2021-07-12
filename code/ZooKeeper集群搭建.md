[TOC]

# ZooKeeper集群搭建



## 一.配置过程

### 1.安装

#### 1）官网下载

> https://downloads.apache.org/zookeeper/

#### 2）上传解压

```bash
tar -zxvf zookeeper3.5.9.tar.gz
```

#### 3）运行

切换到解压目录的bin目录下，执行

```bash
./zkServer.sh
#或
sh zkServer.sh
```

### 1.配置文件

对部分参数进行了注明。

```bash
# The number of milliseconds of each tick
#客户端和服务端保持心跳的毫秒数
tickTime=2000
# The number of ticks that the initial 
# synchronization phase can take
#初始能容忍的心跳数
initLimit=10
# The number of ticks that can pass between 
# sending a request and getting an acknowledgement
#等待最大容忍的心跳数
syncLimit=5
# the directory where the snapshot is stored.
# do not use /tmp for storage, /tmp here is just 
# example sakes.
#快照日志：zookeeper的数据在内存中是以树形结构进行存储的，而快照就是每隔一段时间就会把整个DataTree的数据序列化后存储在磁盘中，这就是zookeeper的快照文件。
dataDir=/apps/apppid/zookeeperData
#事务日志：事务日志指zookeeper系统在正常运行过程中，针对所有的更新操作，在返回客户端“更新成功”的响应前，zookeeper会保证已经将本次更新操作的事务日志已经写到磁盘上，只有这样，整个更新操作才会生效。
dataLogDir=/apps/applogs/zookeeper-logs
#log4j日志在安装目录下配置
# the port at which the clients will connect
#监听端口
clientPort=2181
#新版本通过jetty启动的内嵌管理台
admin.serverPort=8040
#网卡和ip不一致，就是这个导致启动不了，需要加上这行
quorumListenOnAllIPs=true
# the maximum number of client connections.
# increase this if you need to handle more clients
#maxClientCnxns=60
#
# Be sure to read the maintenance section of the 
# administrator guide before turning on autopurge.
#
# http://zookeeper.apache.org/doc/current/zookeeperAdmin.html#sc_maintenance
#
# The number of snapshots to retain in dataDir
#autopurge.snapRetainCount=3
# Purge task interval in hours
# Set to "0" to disable auto purge feature
#autopurge.purgeInterval=1
#最前面是myid,主机ip/服务间心跳连接端口/数据端口
server.1=ip1:2888:3888
server.2=ip2:2888:3888
server.3=ip3:2888:3888

```

### 2.myid文件

在上面配置的dataDir路径下，创建myid文件，里面只需要包含一个唯一数字。（启动时没特殊情况，数字大的会成leader）。

```bash
1
```







## 二.遇到的问题

### 碰到的连环坑

#### 1.开启端口

配置无法启动，开放所有相关UDP,TCP端口依旧没用。

#### 2.jetty端口冲突

观察日志发现高版本引入了jetty控制台,8080端口冲突。仍然无法启动。

#### 3.云服务器网卡

发现云服务器的网卡和ip不一致，需要添加修改字段，此时本地终于可以启动zk服务。

#### 4.配置myid错误

myid全局唯一，每个配置文件都要在dirData路径下存放Myid文件。而且关系要对应上，myid里面的值要和自己
配置文件里配置的值一致，配完发现依旧有问题。

#### 5.未知异常

其他服务器不知道为什么一直出现配置文件异常的告知，但是通过日志又可以看到自己参与了选举。甚至是成为leader，但无法使用status命令，会返回异常信息。

```bash
[root@VM-12-15-centos bin]# sh zkServer.sh status
/usr/bin/java
ZooKeeper JMX enabled by default
Using config: /apps/apache-zookeeper-3.5.9-bin/bin/../conf/zoo.cfg
grep: /apps/apache-zookeeper-3.5.9-bin/bin/../conf/zoo.cfg: No such file or directory
grep: /apps/apache-zookeeper-3.5.9-bin/bin/../conf/zoo.cfg: No such file or directory
mkdir: cannot create directory ‘’: No such file or directory
grep: /apps/apache-zookeeper-3.5.9-bin/bin/../conf/zoo.cfg: No such file or directory
grep: /apps/apache-zookeeper-3.5.9-bin/bin/../conf/zoo.cfg: No such file or directory
grep: /apps/apache-zookeeper-3.5.9-bin/bin/../conf/zoo.cfg: No such file or directory
myid could not be determined, will not able to locate clientPort in the server configs.
Client port not found. Looking for secureClientPort in the static config.
grep: /apps/apache-zookeeper-3.5.9-bin/bin/../conf/zoo.cfg: No such file or directory
Unable to find either secure or unsecure client port in any configs. Terminating.

```

反复修改后，使用sh（./执行会先看第一行是否指定解释器） , 删除dataDir目录下文件，清空日志，修改.sh脚本文件，启动依旧无变化。

#### **6.移动配置文件位置**

把配置文件移动到conf目录下，默认形式启动，不以**命令+配置路径**的方法执行第二台服务器终于启动，并能看到状态。

```bash
#理想结果
[root@VM-12-15-centos bin]# sh zkServer.sh status
/usr/bin/java
ZooKeeper JMX enabled by default
Using config: /apps/apache-zookeeper-3.5.9-bin/bin/../conf/zoo.cfg
Client port found: 2181. Client address: localhost. Client SSL: false.
Mode: follower
```

#### **7.信息不一致**

第三台服务器经过一样的修改后，使用status命令出现的结果有略微不同,使用status返回的是如下结果，暂不清楚解决原因。但猜测应该是云服务器的原因，前两台是腾讯云，这一台的阿里云。 不过对操作没影响，日志里也没异常。

```bash
[root@iZbp17cqlbtdmheyhfmwnmZ bin]# sh zkServer.sh status
/usr/bin/java
ZooKeeper JMX enabled by default
Using config: /apps/apache-zookeeper-3.5.9-bin/bin/../conf/zoo.cfg
Client port found: 2181. Client address: localhost.
commit the final bash!!!!!!!!!!!!!!!
2021-06-04 00:35:29,607 [myid:] - INFO [main:FourLetterWordMain@88] - connecting to localhost 2181 Zookeeper version: 3.5.9-83df9301aa5c2a5d284a9940177808c01bc35cef, built on 01/06/2021 19:49 GMT Latency min/avg/max: 0/0/0 Received: 3 Sent: 2 Connections: 1 Outstanding: 0 Zxid: 0x800000000 Mode: leader Node count: 5 Proposal sizes last/min/max: -1/-1/-1
#不知道为什么 这一行会延迟一下出现
Mode: leader

```

```bash
#连接上zk
sh zkCli.sh -server localhost:2181
#
create /xixikun
#
ls /
#
get /xixikun
```

试了一下，三个结点都能正常运作。任何节点之间都能相互连接，增删改查没啥问题。

从10点整到1点，太难过了，不知道为啥那么多问题，而且log4j日志配置不了日志位置，配置了没用。

## 三.其他

```bash
#如果在安装目录
#./zkServer.sh start /apps/appconf/zookeeper-conf/zoo-2181.cfg

#刷新环境变量
#vi /etc/profile
export ZOOKEEPER_HOME=/apps/apache-zookeeper-3.5.9-bin
export PATH=$PATH:$ZOOKEEPER_HOME/bin
#. /etc/profile

#启动服务
zkServer.sh start

#查询端口监听情况（j）
netstat -lntp
```

