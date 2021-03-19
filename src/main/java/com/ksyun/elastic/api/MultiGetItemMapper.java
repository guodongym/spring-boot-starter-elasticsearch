package com.ksyun.elastic.api;

import org.elasticsearch.action.get.MultiGetItemResponse;

/**
 * 此接口的实现执行将每行数据映射到结果对象，但不需要担心异常处理。
 *
 * @author zhaogd
 */
public interface MultiGetItemMapper<T> {

    /**
     * 对{@link MultiGetItemResponse}进行逐行映射的回调，可以把每一行数据映射为定义的实体
     *
     * @param response es批量get结果
     * @param rowNum   行号
     * @return 映射之后的实体
     * @throws Exception 异常信息
     */
    T mapRow(MultiGetItemResponse response, int rowNum) throws Exception;
}
