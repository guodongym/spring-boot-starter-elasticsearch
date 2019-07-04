package com.scott.elastic.dto;

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

    private Long totalCount;

    private TotalHits.Relation relation;

    private List<T> data;

}
