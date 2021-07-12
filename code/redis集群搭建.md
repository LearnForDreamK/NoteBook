[toc]

## Redis主从，哨兵，集群

### 一.主从复制

> 配置：https://juejin.cn/post/6844903661223542792
> 原理：https://www.cnblogs.com/daofaziran/p/10978628.html （很详细）

#### 1.安装Redis

```bash
cd /usr/local/
wget http://download.redis.io/releases/redis-4.0.11.tar.gz
tar -zxvf redis-4.0.11.tar.gz
cd /usr/local/redis-4.0.11
make install PREFIX=/usr/local/redis
cd /usr/local/redis/bin/
#用于命令行直接运行命令
cp /usr/local/redis-4.0.11/redis.conf /usr/local/
```

#### 2.配置文件

```bash
#----------------------------主服务器
# redis进程是否以守护进程的方式运行，yes为是，no为否(不以守护进程的方式运行会占用一个终端)。
daemonize yes
# 指定redis进程的PID文件存放位置
pidfile /apps/apppid/redis_tx24_6379.pid
# redis进程的端口号
port 6379
# 绑定的主机地址
bind 0.0.0.0
# 客户端闲置多长时间后关闭连接，默认此参数为0即关闭此功能
timeout 300
# redis日志级别，可用的级别有debug.verbose.notice.warning
loglevel verbose
# log文件输出位置，如果进程以守护进程的方式运行，此处又将输出文件设置为stdout的话，就会将日志信息输出到/dev/null里面去了
logfile /apps/applogs/redis-logs/redis_tx24_6379.log
# 设置数据库的数量，默认为0可以使用select <dbid>命令在连接上指定数据库id
databases 16
# 指定在多少时间内刷新次数达到多少的时候会将数据同步到数据文件
#save <seconds> <changes>
# 指定存储至本地数据库时是否压缩文件，默认为yes即启用存储
rdbcompression yes
# 指定本地数据库文件名
dbfilename dump-6379.db
# 指定本地数据问就按存放位置
dir /apps/applogs/redis-logs/
# 指定当本机为slave服务时，设置master服务的IP地址及端口，在redis启动的时候他会自动跟master进行数据同步
#slaveof <masterip> <masterport>
# 当master设置了密码保护时，slave服务连接master的密码
#masterauth <master-password>
# 设置redis连接密码，如果配置了连接密码，客户端在连接redis是需要通过AUTH<password>命令提供密码，默认关闭
requirepass "xixikun"
# 设置同一时间最大客户连接数，默认无限制。redis可以同时连接的客户端数为redis程序可以打开的最大文件描述符，如果设置 maxclients 0，表示不作限制。当客户端连接数到达限制时，Redis会关闭新的连接并向客户端返回 max number of clients reached 错误信息
maxclients 128
# 指定Redis最大内存限制，Redis在启动时会把数据加载到内存中，达到最大内存后，Redis会先尝试清除已到期或即将到期的Key。当此方法处理后，仍然到达最大内存设置，将无法再进行写入操作，但仍然可以进行读取操作。Redis新的vm机制，会把Key存放内存，Value会存放在swap区
#maxmemory<bytes>
# 指定是否在每次更新操作后进行日志记录，Redis在默认情况下是异步的把数据写入磁盘，如果不开启，可能会在断电时导致一段时间内的数据丢失。因为redis本身同步数据文件是按上面save条件来同步的，所以有的数据会在一段时间内只存在于内存中。默认为no。
appendonly no
# 指定跟新日志文件名默认为appendonly.aof
appendfilename appendonly.aof
# 指定更新日志的条件，有三个可选参数 - no：表示等操作系统进行数据缓存同步到磁盘(快)，always：表示每次更新操作后手动调用fsync()将数据写到磁盘(慢，安全)， everysec：表示每秒同步一次(折衷，默认值)；
appendfsync everysec


#----------------------------从服务器
#腾讯云轻量服务器1核2G 从服务器，主服务器ip1.116.106.32
# redis进程是否以守护进程的方式运行，yes为是，no为否(不以守护进程的方式运行会占用一个终端)。
daemonize yes
# 指定redis进程的PID文件存放位置
pidfile /apps/apppid/redis_tx12_16379.pid
# redis进程的端口号
port 16379
# 绑定的主机地址
bind 0.0.0.0
# 客户端闲置多长时间后关闭连接，默认此参数为0即关闭此功能
timeout 300
# redis日志级别，可用的级别有debug.verbose.notice.warning
loglevel verbose
# log文件输出位置，如果进程以守护进程的方式运行，此处又将输出文件设置为stdout的话，就会将日志信息输出到/dev/null里面去了
logfile /apps/applogs/redis-logs/redis_tx12_16379.log
# 设置数据库的数量，默认为0可以使用select <dbid>命令在连接上指定数据库id
databases 16
# 指定在多少时间内刷新次数达到多少的时候会将数据同步到数据文件
#save <seconds> <changes>
# 指定存储至本地数据库时是否压缩文件，默认为yes即启用存储
rdbcompression yes
# 指定本地数据库文件名
dbfilename dump-16379.db
# 指定本地数据问就按存放位置
dir /apps/applogs/redis-logs/
# 指定当本机为slave服务时，设置master服务的IP地址及端口，在redis启动的时候他会自动跟master进行数据同步
slaveof 1.116.106.32 6379
# 当master设置了密码保护时，slave服务连接master的密码
masterauth "xixikun"
# 设置redis连接密码，如果配置了连接密码，客户端在连接redis是需要通过AUTH<password>命令提供密码，默认关闭
requirepass "xixikun"

```

#### 3.运行

```bash
#Server
redis-server /apps/appconf/redis-conf/redis-tx24-6379.conf
redis-cli -p 6379 -a xixikun
#Cli
```

#### 4.中途碰到的小问题

1）.安装配置好后，启动不了服务器，观察日志发现是没有存放数据库的目录，发现redis不会自动创建目录 ，调整配置文件位置即可正常运行。
2）使用redis-cli登录后，发现可以正常进去，权限可以在启动时使用-a加上密码认证，也可以通过先登录再使用auth [密码] 进行验证。
3）INFO replication 可以查看服务器当前状态。
4）redis服务端如果用的不是6379端口，登录客户端需要加上-p [端口号] 参数。
5）想查看.rdb备份文件，但不知道备份文件的具体位置，后经查询说是放在配置文件里设置的dir目录下，进入目录，目录中只有指定配置的.db文件 ， 使用bgsave命令后还是只有这个db文件。使用find命令搜索所有.rdb文件

```bash
find / -name "*.rdb"
[root@VM-12-15-centos /]# find / -name "*.rdb"
/usr/local/redis-4.0.11/tests/assets/hash-zipmap.rdb
/usr/local/redis-4.0.11/tests/assets/encodings.rdb
#只有两行无用的结果
```

但使用bgsave命令后每次dir目录下的.db文件都会更新，最后经过测试后发现.db就是rdb的备份文件。

#### 5.原理

##### 1.大概过程



##### 2.注意的点

1.从服务器也可以有从服务器。
2.可以通过配置让主服务器关闭持久化，提高性能，备份交给从服务器，但有可能因为主服务器重启导致从服务器所有数据清空。
3.主服务器给每个slave维护同步日志和同步标识。slave同步时会带上标识和上次同步的位置。
4.网络状态良好磁盘紧张的情况下如果进行了配置，rdb以master建立socket连接的条件发送。

## 二.哨兵

> 感知不到其他哨兵：https://blog.csdn.net/weixin_44335140/article/details/115049380
>
> 搭建：https://juejin.cn/post/6844903663362637832

### 1.配置哨兵节点

#### 1）配置文件

```properties
# 哨兵sentinel实例运行的端口，默认26379
port 26379
# 哨兵sentinel的工作目录
dir "/apps/applogs/redis-logs"
logfile "/apps/applogs/redis-logs/sentinel-26379.log"
#后台启动
daemonize yes

#redis3.2版本后新增protected-mode配置，默认是yes，即开启。设置外部网络连接redis服务，设置方式如下：
#1、关闭protected-mode模式，此时外部网络可以直接访问
#2、开启protected-mode保护模式，需配置bind ip或者设置访问密码
protected-mode no
bind 0.0.0.0

# 哨兵sentinel监控的redis主节点的
#ip：主机ip地址
#port：端口号
#master-name：可以自己命名的主节点名字（只能由字母A-z、数字0-9 、这三个字符".-_"组成。）
## quorum：当这些quorum个数sentinel哨兵认为master主节点失联 那么这时 客观上认为主节点失联了
# sentinel monitor <master-name> <ip> <redis-port> <quorum>
#注意myid不能相同
#sentinel myid d1c4294864a53fb0ec7b49ba0ca86b6d41470d58

# 当在Redis实例中开启了requirepass <foobared>，所有连接Redis实例的客户端都要提供密码。
# sentinel auth-pass <master-name> <password>
sentinel myid 97790452f0942b748509963ea6483100392b0ea6

# 指定主节点应答哨兵sentinel的最大时间间隔，超过这个时间，哨兵主观上认为主节点下线，默认30秒
# sentinel down-after-milliseconds <master-name> <milliseconds>

# 指定了在发生failover主备切换时，最多可以有多少个slave同时对新的master进行同步。这个数字越小，完成failover所需的时间就越长；反之，但是如果这个数字越大，就意味着越多的slave因为replication而不可用。可以通过将这个值设为1，来保证每次只有一个slave，处于不能处理命令请求的状态。
# sentinel parallel-syncs <master-name> <numslaves>
sentinel deny-scripts-reconfig yes

# 故障转移的超时时间failover-timeout，默认三分钟，可以用在以下这些方面：
## 1. 同一个sentinel对同一个master两次failover之间的间隔时间。
## 2. 当一个slave从一个错误的master那里同步数据时开始，直到slave被纠正为从正确的master那里同步数据时结束。
## 3. 当想要取消一个正在进行的failover时所需要的时间。
## 4.当进行failover时，配置所有slaves指向新的master所需的最大时间。不过，即使过了这个超时，slaves依然会被正确配置为指向master，但是就不按parallel-syncs所配置的规则来同步数据了
# sentinel failover-timeout <master-name> <milliseconds>
sentinel monitor mymaster **.***.**.** 6379 2

# 当sentinel有任何警告级别的事件发生时（比如说redis实例的主观失效和客观失效等等），将会去调用这个脚本。一个脚本的最大执行时间为60s，如果超过这个时间，脚本将会被一个SIGKILL信号终止，之后重新执行。
# 对于脚本的运行结果有以下规则：
## 1. 若脚本执行后返回1，那么该脚本稍后将会被再次执行，重复次数目前默认为10。
## 2. 若脚本执行后返回2，或者比2更高的一个返回值，脚本将不会重复执行。
## 3. 如果脚本在执行过程中由于收到系统中断信号被终止了，则同返回值为1时的行为相同。
## 4.自己自定义一个脚本进行发http请求通知？
# sentinel notification-script <master-name> <script-path>
#sentinel notification-script mymaster /apps/appshells/redis-notify.sh

# 这个脚本应该是通用的，能被多次调用，不是针对性的。
#当一个master由于failover而发生改变时，这个脚本将会被调用，通知相关的客户端关于master地址已经发生改变的信息。以下参数将会在调用脚本时传给脚本:
#<master-name> <role> <state> <from-ip> <from-port> <to-ip> <to-port>
# sentinel client-reconfig-script <master-name> <script-path>
#sentinel client-reconfig-script mymaster /apps/appshells/redis-reconfig.sh


#这是自动生成的！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
# Generated by CONFIG REWRITE
sentinel auth-pass mymaster xixikun
sentinel config-epoch mymaster 0
sentinel leader-epoch mymaster 0
sentinel known-slave mymaster **.***.**.** 16379
sentinel known-slave mymaster **.***.**.** 16379
sentinel known-sentinel mymaster **.***.**.** 26379 d1c4294864a53fb0ec7b49ba0ca86b6d41470d58
sentinel known-sentinel mymaster **.***.**.** 26379 9c126d4a67f7bf3b33c927af083a3682f5e15242
sentinel current-epoch 0

```



#### 2）启动方式

刚开始直接用redis-server 的模式启动，出错了 ， 可以以两种方式启动哨兵。

```bash
redis-server /dir/conf/xxx.conf --sentinel
redis-sentinel /dir/conf/xxx.conf
```



### 2.碰到的问题

#### 1）哨兵感知

配置哨兵时，第一个哨兵配置完成后，直接把把配置文件拷贝到其他两个服务器，三个哨兵启动后，观察日志发现似乎没有引入其他哨兵结点的信息，但引入了主节点和所有从节点的信息 。 查看对应的conf文件发现果然没有自动导入其他哨兵的信息 。 进入哨兵结点，使用info sentinel命令查看。

```bash
sentinel_masters:1
sentinel_tilt:0
sentinel_running_scripts:0
sentinel_scripts_queue_length:0
sentinel_simulate_failure_flags:0
master0:name=mymaster,status=ok,address=1.116.106.32:6379,slaves=2,sentinels=1
```
只有一个哨兵结点，但每个哨兵结点的打印都是这样的情况，按理说，应该是三个哨兵都通过redis的Pub/sub模块完成交流。使用subscribe命令查看__sentinel__:hello 频道发现三个哨兵的myid都相同（先订阅，再启动另外两个sentinel ，否则看不到 ， 还要删除配置文件里自动生成的信息），注释掉另外两个哨兵的Myid重启即解决问题。（我改完以后发现没用，发现我第三个配置好像忘记改了哈哈，改完就好了）

```bash
127.0.0.1:26379> info sentinel
# Sentinel
sentinel_masters:1
sentinel_tilt:0
sentinel_running_scripts:0
sentinel_scripts_queue_length:0
sentinel_simulate_failure_flags:0
master0:name=mymaster,status=ok,address=1.116.106.32:6379,slaves=2,sentinels=3
```

## 三.集群

> 分布式集群（豌豆荚中间件方案Codis）：https://blog.51cto.com/u_12182612/2418723
>
> Codis介绍：https://zhuanlan.zhihu.com/p/50721277
>
> 官方提供方案：https://www.cnblogs.com/maybesuch/p/10309403.html
>
> 好的总结：https://juejin.cn/post/6844903670572646413

>
> 集群模式通常具有 **高可用**、**可扩展性**、**分布式**、**容错** 等特性。一般Redis分布式方案有三种，客户端分区，代理分区，查询路由分区。

### 1.分区方案

#### 1）客户端分区

客户端决定数据被存储到哪一个节点。通过hash算法对key进行hash，特定的key映射到特定的节点，这之间的关系开发者自己处理。在redis集群出现前，redis sharding是客户端分区方案的代表（Jedis支持这个功能）。

##### 优点

- 不需要第三方中间件。

- 分区可控。

- 配置简单。

- 容易线性扩展。

  至于为啥容易线性扩展，我也很疑惑，添加减少机器都会导致重hash，需要移动缓存位置，还要修改所有客户端处的代码，感觉一点都不容易扩展，然后查阅资料发现作者提出一种方案**pre-sharding**，即提前在一台机器上部署多个redis节点，然后客户端处保存所有节点信息，需要扩容时就把某台机器上的一个节点移动到一个新机器上，以达到扩容效果。

  > https://blog.csdn.net/baidu_36161424/article/details/104953504

- 比较灵活。

##### 缺点

- 客户端无法动态增删节点。
- 客户端需要自己维护分发逻辑。（给每个节点维护一个唯一id，然后hash到id直接放入节点内）
- 客户端之间无连接共享，连接浪费。

##### 例子

```java
JedisPoolConfig config = new JedisPoolConfig();
config.setMaxTotal(500);
config.setTestOnBorrow(true);
List<JedisShardInfo> jdsInfoList = new ArrayList<>(2);
jdsInfoList.add(new JedisShardInfo("127.0.0.1", 6379));
jdsInfoList.add(new JedisShardInfo("127.0.0.1", 6380));
pool = new ShardedJedisPool(config, jdsInfoList, Hashing.MURMUR_HASH, Sharded.DEFAULT_KEY_TAG_PATTERN);
jds.set(key, value);
......
jds.close();
pool.close();
```



#### 2）代理分区

客户端发送请求到代理组件，代理解析客户端数据，将请求转发到对应节点，回复结果给客户端。

##### 优点

- 客户端直接接入，简化了客户端逻辑。
- 代理层统一管理分区逻辑，可以偷偷扩容缩容。

##### 缺点

- 多了一层代理，有性能损耗。
- 需要维护这个代理，代理也要注意单点故障问题。

比较出名的有推特的twemproxy和豌豆荚的Codis，twemproxy可以管理redis和memcache，需要结合keepalive和LVS使用。codis则可以以集群部署保证高可用。

##### Codis

Codis以集群模式部署，和redis官方的集群存储节点的思想相同，以槽（SLOT）的方式管理节点，指定总槽数（按需设置），然后每个节点分配一定数量的槽，请求来时通过对key进行hash然后对总槽数取模，找到槽对应的节点，放入指定节点中。 添加减少节点时可以手动移动槽或者自动移动，移动时如果有新的key加入，则会强制这个key转移到新节点，并让codis记录，下次新的key来到这个槽位，这直接转发到新节点。

codis以集群模式部署，用ZooKeeper保存槽和节点对应信息，每个节点监听槽位置的变化。

Codis实现了扫描指令，可以扫描处指定槽下的所有key，方便把这些key移动到新节点。我一直很疑惑是怎么移动槽的，每个槽里有很多key，**但是如何通过槽寻找key呢**？不知道是不是全扫描。

**Codis不支持事务和官方的某些命令，原因就是分布多个的Redis实例没有回滚机制,所以是不支持的。**

#### 3）查询路由

redis官方的集群方案，客户端随机请求集群的一个实例，redis把请求转发给正确的redis节点。（Client请求A节点数据，A告诉客户端去找C，然后客户端请求C节点） 监控管理较为困难，集群节点之间以Gossip协议进行通信。节点恢复过程慢（不及时）。

### 其他

#### 1）节点数

对于官方的集群，刚开始想整两个主节点，每个节点三个哨兵两个从节点，部署时报错，提示集群最少需要3个主节点。

