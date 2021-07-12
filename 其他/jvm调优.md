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

