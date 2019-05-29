package com.scott.elastic.api;

import org.elasticsearch.client.RestHighLevelClient;

/**
 * Es操作模板的回调接口。要与{@link EsTemplate}的执行方法一起使用，通常作为方法实现中的匿名类而使用,不需要关心异常处理
 *
 * @author zhaogd
 */
public interface ClientCallback<T> {

    /**
     * 由{@link EsTemplate}执行，并使用Es客户端进行调用
     *
     * @param client es客户端
     * @return a result object, or null if none
     * @throws Exception thrown by the es api
     */
    T doInClient(RestHighLevelClient client) throws Exception;
}