package com.scott.elastic.dto;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

/**
 * 分页查询参数公用
 *
 * @author zhaogd
 * @date 2019/7/3
 */
@Data
public class ElasticsearchPageRequest {

    /**
     * 排序字段及规则组装
     */
    private FieldSortBuilder[] sortBuilders;

    /**
     * 需要返回的字段
     */
    private String[] sourceIncludes;

    @Range(min = 1, max = 5000, message = "pageNo需要在1和5000之间")
    private Integer pageNo = 1;
    @Range(min = 1, max = 200, message = "pageSize需要在1和200之间")
    private Integer pageSize = 10;

}
