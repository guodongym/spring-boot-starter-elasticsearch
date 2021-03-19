package com.ksyun.elastic.dto;

import lombok.Data;
import org.apache.lucene.search.TotalHits;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author zhaogd
 * @date 2019/7/3
 */
@Data
public class ElasticsearchPageResult<T> {

    /**
     * 总条数
     */
    private Long totalCount;

    /**
     * 总条数的模式，等于或者大于
     */
    private TotalHits.Relation relation;

    /**
     * 滚动id，导出时使用
     */
    private String scrollId;

    /**
     * 结果数据
     */
    private List<T> data;

}
