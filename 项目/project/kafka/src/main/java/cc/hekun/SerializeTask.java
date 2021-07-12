package cc.hekun;

import cc.hekun.action.Callback;
import cc.hekun.action.SerializerMethod;
import cc.hekun.enums.DefaultSerializerEnum;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import org.apache.log4j.Logger;

import java.util.Objects;
import java.util.concurrent.*;

/**
 * 序列化实体基本已经完成使用,任务类是为了提供额外的能力,
 * 序列化前回调,序列化后回调 , 反序列化时可以(加密解密)
 * 还有超时处理,异步调用功能。
 * 维护一个线程池,异步处理任务。
 * @author ouyanghekun
 * @date 2021-06-04 17:35
 */
public class SerializeTask {
    /**
     *
     */
    private static final Logger logger = Logger.getLogger(SerializeTask.class);

    /**
     * 构建线程工厂
     */
    private static final ThreadFactory defaultThreadFactory = ThreadFactoryBuilder.create().setNamePrefix("序列化异步工作线程").setUncaughtExceptionHandler(
            (currentThread,exceptionOrError) -> {
                logger.error("序列化异步工作线程出错",exceptionOrError);
            }
    ).build();

    /**
     * 封装了线程池
     */
    private static final ExecutorService defaultExecutor = new ThreadPoolExecutor(2,2,100,
            TimeUnit.SECONDS,new ArrayBlockingQueue<>(20) , defaultThreadFactory , new ThreadPoolExecutor.DiscardPolicy());


    /**
     *
     */
    private static ThreadFactory threadFactory = defaultThreadFactory;
    /**
     *
     */
    private static ExecutorService executor = defaultExecutor;

    /**
     * 同步调用
     * @param serializerMethod 序列化抽象类子类
     * @param beforeCallback 前置调用
     * @param afterCallback 回调
     * @param obj 对象
     * @param <T> 泛型
     * @return futureTask封装的字节数组
     */
    public static <T> T doExecute(SerializerMethod serializerMethod , Callback beforeCallback , Callback afterCallback, Object obj){
        if(!Objects.isNull(beforeCallback)){
            beforeCallback.callback(obj);
        }
        Object res = serializerMethod.doSerialize(obj);
        if(!Objects.isNull(afterCallback)){
            afterCallback.callback(res);
        }
        return cast(res);
    }
    @SuppressWarnings("uncheck")
    public static <T> T deExecute(SerializerMethod serializerMethod , Callback beforeCallback , Callback afterCallback, Object obj){
        if(!Objects.isNull(beforeCallback)){
            beforeCallback.callback(obj);
        }
        Object res = serializerMethod.deSerialize((byte[]) obj);
        if(!Objects.isNull(afterCallback)){
            afterCallback.callback(res);
        }
        return cast(res);
    }

    @SuppressWarnings("uncheck")
    public static <T> Future<T> doExecuteSync(SerializerMethod serializerMethod , Callback beforeCallback , Callback afterCallback, Object obj){
        Future<Object> submit = executor.submit(() -> {
            if(!Objects.isNull(beforeCallback)){
                beforeCallback.callback(obj);
            }
            Object res = serializerMethod.doSerialize(obj);
            if(!Objects.isNull(afterCallback)){
                afterCallback.callback(res);
            }
            return res;
        });
        return cast(submit);
    }
    @SuppressWarnings("unchecked")
    public static Future<byte[]> doExecuteSync(DefaultSerializerEnum defaultSerializerEnum , Callback beforeCallback , Callback afterCallback, Object obj){
        return doExecuteSync(defaultSerializerEnum.getSerializerMethod() , beforeCallback , afterCallback , obj);
    }
    @SuppressWarnings("unchecked")
    public static Future<Object> deExecuteSync(SerializerMethod serializerMethod , Callback beforeCallback , Callback afterCallback, Object obj){
        Future<Object> submit = executor.submit(() -> {
            if(!Objects.isNull(beforeCallback)){
                beforeCallback.callback(obj);
            }
            Object res = serializerMethod.deSerialize((byte[]) obj);
            if(!Objects.isNull(afterCallback)){
                afterCallback.callback(res);
            }
            return res;
        });
        return cast(submit);
    }
    @SuppressWarnings("unchecked")
    public static <T> Future<T> deExecuteSync(DefaultSerializerEnum defaultSerializerEnum , Callback beforeCallback , Callback afterCallback, Object obj){
        return cast(deExecuteSync(defaultSerializerEnum.getSerializerMethod(),beforeCallback,afterCallback,obj));
    }

    /**
     * 强制类型转化,消除警告
     *
     * @param <T> 转化对象
     * @return 返回对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object object) {
        try {
            return (T) object;
        } catch (ClassCastException e) {
            logger.error("类型转化出错！", e);
        }
        return null;
    }


    public static ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public static void setThreadFactory(ThreadFactory threadFactory) {
        SerializeTask.threadFactory = threadFactory;
    }

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void setExecutor(ExecutorService executor) {
        SerializeTask.executor = executor;
    }
}
