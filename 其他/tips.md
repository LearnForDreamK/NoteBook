#### 项目Tips

##### 1.list类型在redis里面的存储

- 转化成json字符串存储，取出时再转化
- 序列化存储。

##### 2.工具类

如果有某些方法可能很多地方需要用，可以提取为工具类，写一个json转list对象的工具类，通过泛形转化！

```java
//用来判断非空等
StringUtils
//类型转化等，主要是有异常处理
NumberUtils
//时间转化=====很多功能
HutuUtils
//自己写的方便多处使用的工具类
SelfUtils
```



##### 3.构造实体类提供初始化方法

每次都要要显式给新创建的实体类赋初始值，代码比冗余，可以在实体类内部写一个初始化赋值方法。

```java
@Data
@ToString
static class a{
    int a;
    int b;
    public void setInitValue(){
        a = 0;
        b = 0;
    }
}
```

##### 4.修改代码不影响原来服务的正常提供

今天接到通知，要修改原来的一个方法，方法需要添加新的参数，提供一些新的功能，但这个方法同时被很多其他地方使用，而且那些位置不需要这个新的参数和功能，如果直接修改这个方法，可能会影响很多服务，修改很多代码，所以我决定使用重载来解决问题。

```java
//原来的方法，被很多地方使用
public void methodTest(String args1){
    //doSomething
}

//------------------------修改后
public void methodTest(String args1){
    methodTest(args1,null);
}
//添加额外功能的方法
private void methodTest(String args1,String args2){
    //doSomething
    //额外功能
    if(!Objects.isNull(args2)){
       //more action 
    }
}
```

