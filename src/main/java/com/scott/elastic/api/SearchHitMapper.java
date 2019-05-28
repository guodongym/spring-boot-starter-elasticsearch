package com.scott.elastic.api;

import org.elasticsearch.search.SearchHit;

/**
 * 此接口的实现执行将每行数据映射到结果对象，但不需要担心异常处理。
 *
 * @author zhaogd
 */
public interface SearchHitMapper<T> {

    /**
     * 对{@link SearchHit}进行逐行映射的回调，可以把每一行数据映射为定义的实体
     *
     * @param searchHit 搜索结果
     * @param rowNum    行号
     * @return 映射之后的实体
     * @throws Exception 异常信息
     */
    T mapRow(SearchHit searchHit, int rowNum) throws Exception;
}
