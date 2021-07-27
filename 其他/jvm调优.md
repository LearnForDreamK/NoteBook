1.今天想用jps查看一下服务器上运行的java程序，怎么都运行不起来。
刚开始以为是环境变量的问题，照着网上各种配法一个一个配，因为以前是用yum安装的，经过各路查找，找到安装路径/usr/lib/jvm ， 但是怎么配置执行jps依旧是找不到命令。
2.弄了很长时间后，我感觉不是环境变量的问题，因为我安装的我open-jdk，所以我觉得可能是open-jdk的问题，果然，查询关键字openjdk-jps发现，相关工具似乎要安装一个open-jdk-devel包，但网上只能查到JDK8版本的这个包安装方法

```bash
yum install -y  java-1.8.0-openjdk-devel
```

我尝试把1.8.0替换为我在刚才jvm目录下的版本,均提示找不到包

```bash
yum install -y  java-11.0.11.0.9-openjdk-devel
yum install -y  java-11.0.11-openjdk-devel
```

使用下面命令搜索，搜索出海量不相关的结果

```bash
yum search java ｜grep jdk-devel
```

在google上查询后，发现似乎有i相同问题的人，看到可以直接替换为11

```bash
yum install -y  java-11-openjdk-devel
```

终于执行成功了。然后执行JPS命令，终于可以了！！！！！！！！





## FullGC排查

1.jmap先看看对象占比。

可能是年轻代对象跳到老年代的阈值设定太小。

```shell
#jps找进程id
jps
#找到id查询jvm参数
ps -ef | grep id
#jmap查询堆内内存分配情况
jmap -heap id | head -nXXX
#查看堆中对象情况
#jdk8用的下面这种
jmap -histo 15982 | head -n10
#--histo代表查看堆内对象
jhsdb jmap --histo --pid  15982 | head -n20

#导出dump文件 是stw的
jmap -dump:format=b,file=heap idNumber
#使用jvisualVM工具分析 或者 MAT工具分析

```

对于元空间部分，可以从加载类方向考虑，元空间耗尽时（自己限制），也会频繁触发fullgc。jvm启动时，添加下面指令。

```shell
-verbose:class #查看类加载情况
-verbose:gc #查看虚拟机中内存回收情况
-verbose:jni #查看本地方法调用的情况
```

可以看看是不是重复加载了大量类导致的fullgc频繁。



## 2.cpu排查

先找出cpu占用高的进程

```shell
#进程列表 按P排序
top -c
#找出进程下面的线程
top -Hp id
#通过进程id看看进程在干嘛，后面是转储到文件里，可以直接查看
jstack -l 2609 > ./2609.stack
#cat 文件看看那些线程一直在跑
```

通过阿里的Arthas命令检查

```shell
#下载
curl -L http://start.alibaba-inc.com/install.sh | sh
#运行
./as.sh
#查看CPU占用排行前三的线程
thread -n 3 -i 1000
```

