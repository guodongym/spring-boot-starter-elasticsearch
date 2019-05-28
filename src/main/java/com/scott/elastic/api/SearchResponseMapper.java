package com.scott.elastic.api;

import org.elasticsearch.action.search.SearchResponse;

/**
 * 将检索结果进行实体映射
 *
 * @author zhaogd
 */
public interface SearchResponseMapper<T> {

    /**
     * 对{@link SearchResponse}进行逐行映射的回调，需要自行处理成一个实体列表
     *
     * @param searchResponse 搜索结果，包含聚合等信息
     * @return 映射之后的实体
     * @throws Exception 异常信息
     */
    T mapRow(SearchResponse searchResponse) throws Exception;
}
