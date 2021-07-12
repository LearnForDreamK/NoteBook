#### AOP简单笔记

aspect object programming，面向切面编程。平常代码中，有很多重复的部分，可以称为**关注点**代码，然后通过某种方式把关注点代码抽取出来，和业务代码分离，然后运行业务代码的时候动态植入关注点代码。 关注点形成的类，可以称为**切面类**。 通过**切入点表达式**，拦截哪些类的哪些方法。 还可以用于增强方法，对方法改造，增强。

```java
@Aspect                            //指定一个类为切面类

@Pointcut("execution(* cc.hekun..(..))")  //指定切入点表达式

@Before("pointCut_()")                //前置通知: 目标方法之前执行

@After("pointCut_()")                //后置通知：目标方法之后执行（始终执行）

@AfterReturning("pointCut_()")            //返回后通知： 执行方法结束前执行(异常不执行)

@AfterThrowing("pointCut_()")            //异常通知:  出现异常时候执行

@Around("pointCut_()")                //环绕通知： 环绕目标方法执行

// 指定切入点表达式，拦截哪个类的哪些方法
@Pointcut("execution(* aa.*.*(..))")
public void pointCutMethod() {
}
//例子
@Before("pointCutMethod)")
public void begin() {
	System.out.println("开始事务");
}

//表达式格式
execution(modifiers-pattern? ret-type-pattern declaring-type-pattern? name-pattern(param-pattern) throws-pattern?)
//例如,以public修饰 任意返回值,cc.hekun包下任意类的任意方法
execution(public * cc.hekun.* *(...) )
符号讲解：

?号代表0或1，可以不写

“*”号代表任意类型，0或多

方法参数为..表示为可变参数

参数讲解：

modifiers-pattern?【修饰的类型，可以不写】

ret-type-pattern【方法返回值类型，必写】

declaring-type-pattern?【方法声明的类型（包名），可以不写】

name-pattern(param-pattern)【要匹配的名称，括号里面是方法的参数】

throws-pattern?【方法抛出的异常类型，可以不写】


//通过环绕通知,拿到拦截方法的信息
@Aspect("拦截表达式")
public Object aroundAspect(ProceedingJoinPoint joinPoint){
    //ProceedingJoinPoint可以拿到拦截方法信息
}
```

#### AOP实现系统流水日志监控

因为收到需求，需要一种方式记录整个系统操作相关日志，所以第一时间想到AOP,为了记录的灵活性，所以使用注解实现，在需要监控的方法上使用自定义注解即可，通过自定义注解+AOP 监控整个系统流水日志 , 结合之前说的拦截器，从上下文获取用户信息，记录用户操作记录，时间，入参出参等，并留下扩展格式，预留级别，让用户能自定义日志格式输出信息。

##### 1.自定义注解

```java
/**
 * 系统操作流水日志,记录所有人员的操作记录日子
 * 扩展内容START格式：包括n个{},代表方法名，类名，时间什么的
 *       END格式：包括n个{}
 * 格式自定 "             "
 * 切面控制类:LogAspect
 * @author hekun
 * @date 2021-05-10 19点36分
 */
//生成文档时会有这个注解
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
//这个注解标注的注解只有在类上使用时才会有作用，标识子类会继承这个注解
@Inherited
public @interface RunningLog {
    //默认记录级别为0(这个字段留着备用)
    int level() default 0;
    //自定义起始 -------> 默认请求开始拼接内容
    String start() default "";
    //自定义结束 -------> 默认请求结束拼接内容
    String last() default "";
    //自定义异常语句,只是打印自定义的语句,不会提供附加参数
    String error() default "";
}

```

##### 2.切入点

注解中留下了自定义格式日志的属性，让用户可以自己自定义日志格式。

```java
@RunningLog(start = "" , last = "" , error = "" )
```



```java
/**
 * 全局日志控制类
 * @author hekun
 * @date 2021-05-10 20点36分
 */
@Component
@Aspect
@Slf4j
public class LogAspect {
    @Around("@annotation(注解全类名)")
    public Object aroundAspect(ProceedingJoinPoint joinPoint) throws Throwable{
        if (!(joinPoint.getSignature() instanceof MethodSignature)) {
            log.error("签名异常,注解 @RunningLog 不在方法上-------------->  ");
            return joinPoint.proceed();
        }
        //方法调用时间计算
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object[] args = joinPoint.getArgs();
        //获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        //获取注解信息
        RunningLog runningLog = method.getAnnotation(RunningLog.class);
        String methodStartHandler = "默认初始化调用起始格式" ,
               methodEndHandler = "默认结束调用格式";
        if (log.isInfoEnabled()) {
            //自定义起始日志
            if(!StringUtils.isBlank(runningLog.moreHead())){
                methodStartHandler = runningLog.moreHead();
            }
            log.info(methodStartHandler,
                    method.getDeclaringClass().getName(), method.getName(),
                    StringUtils.join(args, ","), DateUtil.now());
        }
        Object result = null;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            log.error("接口调用 ----->调用出错 ", e);
            //扩展日志
            if(StringUtils.isBlank(runningLog.error())){
                log.error(runningLog.error());
            }
            throw e;
        } finally {
            if (log.isInfoEnabled()) {
                //时间停止计数
                stopWatch.stop();
                //自定义结束日志
                if(!StringUtils.isBlank(runningLog.moreTail())){
                    methodEndHandler = runningLog.moreTail();
                }
                log.info(methodEndHandler,可变参数);
            }
        }
        return result;
    }
}
```



#### 参考文章

https://juejin.cn/post/6844903609889456142#heading-1

https://mp.weixin.qq.com/s?__biz=MzI4Njg5MDA5NA==&mid=2247483954&idx=1&sn=b34e385ed716edf6f58998ec329f9867&chksm=ebd74333dca0ca257a77c02ab458300ef982adff3cf37eb6d8d2f985f11df5cc07ef17f659d4#rd

