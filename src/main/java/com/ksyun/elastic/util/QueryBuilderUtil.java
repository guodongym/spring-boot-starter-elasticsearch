package com.ksyun.elastic.util;

import com.google.common.collect.Lists;
import com.ksyun.elastic.dto.FieldSortBuilder;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.time.ZoneId;
import java.util.List;

/**
 * es查询条件构建工具
 *
 * @author zhaogd
 * @date 2020/2/6
 */
public class QueryBuilderUtil {

    /**
     * 构建日期区间查询条件
     *
     * @param queryBuilder 查询条件构建器
     * @param fieldName    字段名
     * @param startTime    开始时间
     * @param endTime      结束时间
     */
    public static void buildTimeRangeFilterQuery(BoolQueryBuilder queryBuilder, String fieldName, String startTime, String endTime) {
        if (!StringUtils.isAllBlank(startTime, endTime)) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(fieldName);
            if (StringUtils.isNotBlank(startTime)) {
                rangeQuery.gte(startTime);
            }
            if (StringUtils.isNotBlank(endTime)) {
                rangeQuery.lte(endTime);
            }
            rangeQuery.timeZone(ZoneId.systemDefault().getId());
            queryBuilder.filter(rangeQuery);
        }
    }

    public static SortBuilder<?>[] buildSort(FieldSortBuilder[] sortBuilders) {
        List<SortBuilder<?>> sort = Lists.newArrayList();
        if (sortBuilders != null && sortBuilders.length > 0) {
            for (FieldSortBuilder fieldSortBuilder : sortBuilders) {
                final String name = fieldSortBuilder.getFieldName();
                final SortOrder sortOrder = fieldSortBuilder.getSortOrder();
                sort.add(SortBuilders.fieldSort(name).order(sortOrder));
            }
        }
        return sort.toArray(new SortBuilder<?>[]{});
    }
}


