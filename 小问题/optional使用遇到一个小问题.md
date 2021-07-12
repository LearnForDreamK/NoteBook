#### 1.没用的操作

今天在写一个更新接口，因为是运营端，所以没有必要使用RPC和网关，直接用springboot写接口。 然后更新可能只更新一个或多个字段，有三种方法进行更新。

- 让前台更新提交时，带上所有参数，包括没更新的参数。
- 后台提供专门对应的更新接口。（太繁琐）
- 前台只提供更新的参数，后台进行对应的空值处理。(使用tkmytatis，空值属性默认会被跳过 )

我使用的是第三种，在写RPC接口时，网关提供的参数经过Json转换工具类转换并包装为对象后，没有赋值的属性会被工具类用null初始化。 但是今天使用springboot时，没有传的参数springboot并不会进行初始化，所以需要进行非空判断，optional类正好可以用来解决这个问题。 

```java
//如果普通的判断，每个属性要自行判断
Object a = new Object();
Object res = new Object();
if(a.getProperty != null){
    res.setProperty(a.getProperty());
}
//优雅一点
res.setP1(a.getProperty() == null?null:a.getProperty());
//更优雅一点
//我第一次这样写写错了,想着是如果为空,就会拿到一个普通的Null
Optional.ofNullable(a.getProperty()).orElseGet(null);
//发现这个方法参数只支持一个lambda表达式,传null进去也不会报错! 运行时百分百报错,下面才是正确的
Optional.ofNullable(a.getProperty()).orElseGet(()->{return null;});
//或者这样写
Optional.ofNullable(a.getProperty()).orElse(null);
/**
* 区别是，无论optional是否有值,orElse里面都会执行,
* orElseGet只会在optional为空时执行。
*
*/
//如果有类型需要转换，比如把整形转化为布尔类型
a.getProperty()==null?null:a.getProperty()==0;
//byte转int类型
Integer a = Optional.ofNullable(a.getProperty()).orElseGet(()->{return null;});
res.set(a==null?null:a.byteValue());
```

#### 2.最后发现完全是多此一举

经过测试，没有赋值的属性通过get方法获取能获取到null，并不会报空指针异常。 出现空指针的原因就是

```java
a.getProperty().byteValue()==null?null:a.getProperty().byteValue();
```

调用byteValue的时候出现空指针! QAQ。不过就当熟悉了一下Optional了。

正常使用直接赋值即可。

```
res.setP1(a.getProperty());
```

~~最后仔细一想，没必要中间赋值，直接使用po层实体让springmvc自己赋值。上面所有步骤都不需要了。~~

#### 