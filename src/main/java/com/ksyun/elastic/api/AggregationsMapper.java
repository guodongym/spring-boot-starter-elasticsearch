package com.ksyun.elastic.api;

import org.elasticsearch.search.aggregations.Aggregations;

/**
 * 将聚合结果进行实体映射
 *
 * @author zhaogd
 */
public interface AggregationsMapper<T> {

    /**
     * 对{@link Aggregations}进行逐行映射的回调，需要自行处理成一个实体列表
     *
     * @param aggregations 聚合结果
     * @return 映射之后的实体
     * @throws Exception 异常信息
     */
    T mapRow(Aggregations aggregations) throws Exception;
}
