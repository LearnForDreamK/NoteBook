# SpringCloudProjects
springcloud实例练习项目

#踩坑记录

### **项目名：cloud-consumer-order-80**

1.在自己实现LoadBalanced算法过程中 忘记将初始在向IOC容器注入RestTemplate的配置中的@LoadBalanced注解注销掉



### **项目名：cloud-consumer-feign-order-80**

1.启动项目出错，发现注册eureka的地址有误（本地DNS配置地址127.0.0.1 www.eureka700X.com）

2.ribbon超时配置，在application文件中配置全局ribbion超时时间时，发现idea没有任何提示，无法识别ribbon的这些属性（可能
因为ribbon是间接导入的？？），但是按照说明配置后，启动项目后发现配置有用

### **项目名：cloud-provider-hystrix-payment8001**

1.在配置hystrix熔断器时，用的@HystrixComment注解，里面有commandProperties这一数组，里面存放的是key value类的属性值，很好奇
这些属性值从哪里可以看到，通过搜索（shift+shift）发现了HystrixPropertiesCommandDefault extends HystrixCommandProperties,
下载HystrixCommandProperties的源码 可以看到这些属性值相关注释了解属性是干嘛的

2.熔断器在发生异常时没有进行服务降级操作，发现主启动类没有添加@EnableHystrix （思维导图没有。。。。）

3.在测试时ribbon的时间限制记得关注一下，如果测试的时间过长，ribbion也会引起服务降级，让服务无法成功运行，导致服务永远处于降级状态

### **项目名**：cloud-consumer-hystrix-dashboard9001

1.导入依赖时 ,发现无法导入这个依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
</dependency>
```

这个依赖需要配合两个其他依赖 ，这个时候 ，就发现上面的依赖已经成功导入了

```xml
<dependency>
    <groupId>com.netflix.hystrix</groupId>
    <artifactId>hystrix-javanica</artifactId>
    <version>RELEASE</version>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-netflix-hystrix-dashboard</artifactId>
</dependency>
```