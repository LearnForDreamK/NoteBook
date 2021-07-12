##### **遇到的问题**

有一个对象集合的查询评论非常高，而且对实时性有一定的要求，对于如何缓存，我找到两种方案。

##### 1.JsonUtil

将对象的List集合转化为Json格式的String对象，然后直接存储在redis中。 如果这个集合有更新或者添加，则更新这个缓存。 读取时，拿出jsonstring，然后转化为集合对象。

```java
    public static <T> List<T> getListFromJsonString(String jsonString,Class<?> clazz){
        List<T> res= new ArrayList<T>();
        try {
            res = (List<T>) JSON.parseArray(jsonString,clazz);
        } catch (Exception e) {
            log.error("JsonToObjectUtil Error",e);
        }
        return res;
    }
```



###### 1）<u>可能会出现的问题</u>

如果更新，可能会出现缓存击穿问题，大量请求在更新缓存时转到数据库。

###### 2)一种解决方案

在集群中，每台设备维护自己的JVM缓存，通过定时任务定期从redis中获取最新缓存数据更新到自己本地缓存中。 这样还可以减少json字串和对象之间的转化次数，提高性能，分摊压力。  现在还没遇到场景，所以以后再通过这种方式实现。 定时调度可以通过分布式定时调度任务框架elastic job去实现，给不同的机器设置差距不大的不同的更新时间。



##### 2.其他序列化方案

最近在学netty相关知识，接触了一些序列化知识，jdk的序列化得到的byte数组大于json转换成字符串的byte数组。 还有其他多种序列化方案。 网上查询json转字符串存储在redis中效率比对象通过序列化方式转化为byte数组低一些。 ==写一个序列化工具类，把对象转化为byte[]数组。 印象中redis存储字符串会根据传入的值选择不同的结构体。

