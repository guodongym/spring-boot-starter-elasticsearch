package com.ksyun.elastic.dto;

import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.search.sort.SortOrder;

/**
 * 排序字段及规则组装
 *
 * @author zhaogd
 * @date 2019/7/12
 */
@Getter
@Setter
public class FieldSortBuilder {

    /**
     * 排序字段名称(导出时不可用)
     */
    private String fieldName;

    /**
     * 升降序列（ASC,DESC）(导出时不可用)
     */
    private SortOrder sortOrder = SortOrder.ASC;
}
