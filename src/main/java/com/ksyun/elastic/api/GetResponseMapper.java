package com.ksyun.elastic.api;

import org.elasticsearch.action.get.GetResponse;

/**
 * 此接口的实现执行将每行数据映射到结果对象，但不需要担心异常处理。
 *
 * @author zhaogd
 */
public interface GetResponseMapper<T> {

    /**
     * 对{@link GetResponse}进行映射的回调，可以把数据映射为定义的实体
     *
     * @param response get结果
     * @return 映射之后的实体
     * @throws Exception 异常信息
     */
    T mapRow(GetResponse response) throws Exception;
}
