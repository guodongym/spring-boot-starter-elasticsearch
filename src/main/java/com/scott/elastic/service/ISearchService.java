package com.scott.elastic.service;

import com.scott.elastic.dto.BatchDoc;
import com.scott.elastic.dto.IndexModel;
import com.scott.elastic.dto.SearchModel;
import com.scott.elastic.dto.SingleDoc;

import java.util.ArrayList;
import java.util.Map;


public interface ISearchService {

    Boolean createIndex(IndexModel indexModel);  // 创建索引
    Boolean existIndex(String indexName);       // 判断索引是否存在
    Boolean deleteIndex(String indexName);      // 删除索引
    Boolean updateIndex();                        // 更新索引：该接口暂未实现

    Boolean insertDoc(SingleDoc singleDoc);     // 插入单个文档
    Boolean insertDocBatch(BatchDoc batchDoc);  // 批量插入多个文档
    ArrayList<Map<String, Object>> queryDocs(SearchModel searchModel);  // 查询文档
    Boolean deleteDoc(SingleDoc singleDoc);     // 删除单个文档
    Boolean deleteDocBatch(BatchDoc batchDoc);  // 批量删除文档
    Boolean updateDoc(SingleDoc singleDoc);     // 更新单个文档
}
