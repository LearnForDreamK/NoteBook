package cc.hekun.action;

public interface Callback {

    /**
     * 回调后的字节数组,对method的返回值进行处理
     * @param res
     */
    void callback(Object res);


}
