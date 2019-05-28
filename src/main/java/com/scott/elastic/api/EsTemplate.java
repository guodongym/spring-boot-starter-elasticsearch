package com.scott.elastic.api;

import com.scott.elastic.dto.IndexDoc;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * es操作模板类
 *
 * @author zhaogd
 * @date 2019/5/28
 */
@Slf4j
@AllArgsConstructor
public class EsTemplate implements EsOperations {

    private RestHighLevelClient client;
    private BulkProcessor bulkProcessor;


    @Override
    public Boolean indicesExists(String... indices) throws IOException {
        GetIndexRequest request = new GetIndexRequest(indices);
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }

    @Override
    public <T> T execute(ClientCallback<T> action) {
        Assert.notNull(action, "Callback object must not be null");

        try {
            return action.doInClient(client);
        } catch (Throwable throwable) {
            log.error("es执行出错", throwable);
            throw new RuntimeException(throwable);
        }
    }


    @Override
    public <T> List<T> get(String index, MultiGetItemMapper<T> mapper, String... ids) {
        final MultiGetRequest request = new MultiGetRequest();
        for (String id : ids) {
            request.add(index, id);
        }

        log.info("MultiGetRequest Items: [{}]", request.getItems());
        return this.execute(client -> {
            final MultiGetResponse response = client.mget(request, RequestOptions.DEFAULT);

            List<T> rs = new ArrayList<>();
            int rowNum = 0;
            for (MultiGetItemResponse itemResponse : response.getResponses()) {
                rs.add(mapper.mapRow(itemResponse, rowNum++));
            }
            return rs;
        });
    }


    @Override
    public <T> List<T> search(SearchHitMapper<T> mapper, SearchRequest searchRequest) {
        log.info("send request json:{}", searchRequest.toString());
        return this.execute(client -> {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            List<T> rs = new ArrayList<>();
            int rowNum = 0;
            for (SearchHit hit : searchResponse.getHits()) {
                rs.add(mapper.mapRow(hit, rowNum++));
            }

            return rs;
        });
    }


    @Override
    public <T> List<T> searchAll(SearchHitMapper<T> mapper, int size, String... indices) {
        SearchRequest searchRequest = new SearchRequest(indices);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(size);

        searchRequest.source(searchSourceBuilder);

        return this.search(mapper, searchRequest);
    }

    @Override
    public <T> List<T> searchDocs(QueryBuilder queryBuilder, SortBuilder sort, String[] sourceIncludes, Integer pageNo, Integer pageSize,
                                  SearchHitMapper<T> mapper, String... indices) {
        SearchRequest searchRequest = new SearchRequest(indices);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder)
                .fetchSource(sourceIncludes, null)
                .sort(sort)
                .from((pageNo - 1) * pageSize)
                .size(pageSize);

        searchRequest.source(searchSourceBuilder);

        return this.search(mapper, searchRequest);
    }


    @Override
    public <T> T searchIndexAndAggs(QueryBuilder queryBuilder, SortBuilder sort, Integer pageNo, Integer pageSize,
                                    AggregationBuilder aggregationBuilder, SearchResponseMapper<T> mapper, String... indices) {

        SearchRequest searchRequest = new SearchRequest(indices);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder)
                .sort(sort)
                .from((pageNo - 1) * pageSize)
                .size(pageSize)
                .aggregation(aggregationBuilder);

        searchRequest.source(searchSourceBuilder);

        log.info("send request json:{}", searchRequest.toString());
        return this.execute(client -> {
            final SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            return mapper.mapRow(searchResponse);
        });
    }


    @Override
    public boolean bulk(BulkRequest bulk) {
        return this.execute(client -> {
            BulkResponse bulkResponse = client.bulk(bulk, RequestOptions.DEFAULT);

            if (bulkResponse.hasFailures()) {
                log.error("批量处理失败 {}", bulkResponse.buildFailureMessage());
                return false;
            }
            log.info("处理{}条记录,耗时:{}ms", bulk.numberOfActions(), bulkResponse.getTook().getMillis());
            return true;
        });
    }

    @Override
    public boolean addDoc(String index, IndexDoc... docs) {
        BulkRequest bulk = new BulkRequest(index);
        bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        for (IndexDoc doc : docs) {
            final IndexRequest request = new IndexRequest();
            if (StringUtils.isNotBlank(doc.getId())) {
                request.id(doc.getId());
            }
            request.source(doc.getJsonString(), XContentType.JSON);

            bulk.add(request);
        }

        return this.bulk(bulk);
    }


    @Override
    public boolean updateDocByScript(String index, Script script, String... ids) {
        BulkRequest bulk = new BulkRequest(index);
        bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        for (String id : ids) {
            final UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.id(id)
                    .script(script)
                    .retryOnConflict(3);

            bulk.add(updateRequest);
        }

        return this.bulk(bulk);
    }

    @Override
    public boolean updateDoc(String index, IndexDoc... docs) {
        BulkRequest bulk = new BulkRequest(index);
        bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        for (IndexDoc doc : docs) {
            final UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.id(doc.getId())
                    .doc(doc.getJsonString(), XContentType.JSON)
                    .retryOnConflict(3);

            bulk.add(updateRequest);
        }
        return this.bulk(bulk);
    }

    @Override
    public boolean deleteDoc(String index, String... ids) {
        BulkRequest bulk = new BulkRequest(index);
        bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        for (String id : ids) {
            final DeleteRequest deleteRequest = new DeleteRequest().id(id);

            bulk.add(deleteRequest);
        }
        return this.bulk(bulk);
    }


    @Override
    public boolean updateByQuery(String index, QueryBuilder queryBuilder, Script script) {
        UpdateByQueryRequest request = new UpdateByQueryRequest(index);
        request.setQuery(queryBuilder)
                .setScript(script)
                .setRefresh(true);

        log.info("send request json :" + request.toString());
        return this.execute(client -> {
            BulkByScrollResponse bulkResponse =
                    client.updateByQuery(request, RequestOptions.DEFAULT);

            log.info("通过查询更新结果：{}", bulkResponse.toString());
            List<BulkItemResponse.Failure> bulkFailures = bulkResponse.getBulkFailures();
            List<String> errorIds = new ArrayList<>();
            for (BulkItemResponse.Failure bulkFailure : bulkFailures) {
                errorIds.add(bulkFailure.getId());
                log.warn("查询更新失败：[ id : " + bulkFailure.getId() + ";" + "status : " + bulkFailure.getStatus() + ";" + "message : " + bulkFailure.getMessage() + "]");
            }

            if (errorIds.isEmpty()) {
                return true;
            }

            log.info("更新失败数据补偿：{}", errorIds);
            return this.updateDocByScript(index, script, errorIds.toArray(new String[]{}));
        });
    }


    /**
     * 聚合查询
     *
     * @param indices            索引名称
     * @param queryBuilder       检索条件
     * @param aggregationBuilder 聚合条件
     * @return 聚合结果
     */
    public <T> T aggregation(AggregationsMapper<T> mapper, QueryBuilder queryBuilder, AggregationBuilder aggregationBuilder, String... indices) {
        SearchRequest searchRequest = new SearchRequest(indices);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder)
                .size(0)
                .aggregation(aggregationBuilder);

        searchRequest.source(searchSourceBuilder);
        log.info("send request json:{}", searchRequest.toString());
        return this.execute(client -> {
            final SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            return mapper.mapRow(searchResponse.getAggregations());
        });
    }

//    /**
//     * 滚动查询
//     *
//     * @param queryBuilder 查询条件
//     * @param indices      索引名称
//     * @return 结果
//     */
//    public SearchResponse searchByScroll(QueryBuilder queryBuilder, String[] sourceIncludes, SliceBuilder sliceBuilder, String... indices) {
//        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(indices)
//                .setQuery(queryBuilder)
//                .setSize(50000)
//                .addSort(SortBuilders.fieldSort("_doc"))
//                .slice(sliceBuilder)
//                .setScroll(TimeValue.timeValueSeconds(20))
//                .setFetchSource(sourceIncludes, null);
//
//        logger.info("send request json:{}", searchRequestBuilder.toString());
//        return searchRequestBuilder.get();
//    }
//
//    /**
//     * 根据滚动ID获取数据
//     *
//     * @param scrollId 滚动ID
//     * @return 单次滚动结果
//     */
//    public SearchResponse searchByScrollId(String scrollId) {
//        SearchScrollRequestBuilder searchScrollRequestBuilder = client.prepareSearchScroll(scrollId)
//                .setScroll(TimeValue.timeValueSeconds(20));
//        logger.info("searchByScrollId scrollID {}", scrollId);
//        return searchScrollRequestBuilder.get();
//    }
//
//
//    /**
//     * 加入索引请求到缓冲池
//     *
//     * @param indexName  索引名称
//     * @param jsonString 索引实体
//     */
//    public void addIndexRequestToBulk(String indexName, String jsonString) {
//        try {
//            IndexDoc indexRequest = new IndexDoc(indexName, elasticSearchConfig.getType()).source(jsonString, XContentType.JSON);
//            bulkProcessor.add(indexRequest);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    /**
//     * 加入索引请求到缓冲池
//     *
//     * @param indexName 索引名称
//     * @param id        ID
//     * @param json      实体
//     */
//    public void addIndexRequestToBulk(String indexName, String id, Map<String, Object> json) {
//        try {
//            IndexDoc indexRequest = new IndexDoc(indexName, elasticSearchConfig.getType(), id).source(json);
//            bulkProcessor.add(indexRequest);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 加入索引请求到缓冲池
//     *
//     * @param indexName  索引名称
//     * @param id         ID
//     * @param jsonString 索引实体
//     */
//    public void addIndexRequestToBulk(String indexName, String id, String jsonString) {
//        try {
//            IndexDoc indexRequest = new IndexDoc(indexName, elasticSearchConfig.getType(), id).source(jsonString, XContentType.JSON);
//            bulkProcessor.add(indexRequest);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 加入删除请求到缓冲池
//     *
//     * @param indexName 索引名称
//     * @param id        ID
//     */
//    public void addDeleteRequestToBulk(String indexName, String id) {
//        try {
//            DeleteRequest deleteRequest = new DeleteRequest(indexName, elasticSearchConfig.getType(), id);
//            bulkProcessor.add(deleteRequest);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 加入更新请求到缓冲池
//     *
//     * @param indexName   索引名
//     * @param id          id
//     * @param json        更新请求体
//     * @param docAsUpsert 不存在时是否新增
//     */
//    public void addUpdateRequestToBulk(String indexName, String id, Map<String, Object> json, boolean docAsUpsert) {
//        try {
//            UpdateRequest updateRequest = new UpdateRequest(indexName, elasticSearchConfig.getType(), id)
//                    .doc(json)
//                    .docAsUpsert(docAsUpsert)
//                    // 更新冲突时重试次数
//                    .retryOnConflict(3);
//            bulkProcessor.add(updateRequest);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 加入更新请求到缓冲池
//     *
//     * @param indexName   索引名
//     * @param id          id
//     * @param jsonString  更新请求体
//     * @param docAsUpsert 不存在时是否新增
//     */
//    public void addUpdateRequestToBulk(String indexName, String id, String jsonString, boolean docAsUpsert) {
//        try {
//            UpdateRequest updateRequest = new UpdateRequest(indexName, elasticSearchConfig.getType(), id)
//                    .doc(jsonString, XContentType.JSON)
//                    .docAsUpsert(docAsUpsert)
//                    // 更新冲突时重试次数
//                    .retryOnConflict(3);
//            bulkProcessor.add(updateRequest);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    /**
//     * 手动刷新索引
//     *
//     * @param indices 索引名称
//     */
//    public void refresh(String indices) {
//        client.admin().indices().refresh(new RefreshRequest(indices)).actionGet();
//    }
//
//
//    /**
//     * 关闭连接
//     */
//    public static void close() {
//        try {
//            bulkProcessor.awaitClose(5, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            logger.error(e.getMessage(), e);
//        } finally {
//            client.close();
//        }
//    }
//
//    /**
//     * 手动刷新批处理器
//     */
//    public static void flush() {
//        bulkProcessor.flush();
//    }

}
