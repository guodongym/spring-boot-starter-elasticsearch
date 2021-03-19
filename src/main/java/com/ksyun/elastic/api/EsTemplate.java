package com.ksyun.elastic.api;

import com.ksyun.elastic.constants.Constants;
import com.ksyun.elastic.dto.ElasticsearchPageResult;
import com.ksyun.elastic.dto.IndexDoc;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    public Boolean indicesExists(String... indices) {
        GetIndexRequest request = new GetIndexRequest(indices);
        return this.execute(client -> client.indices().exists(request, RequestOptions.DEFAULT));
    }

    @Override
    public <T> T execute(ClientCallback<T> action) {
        Assert.notNull(action, "Callback object must not be null");

        try {
            return action.doInClient(client);
        } catch (Exception e) {
            log.error("es执行出错", e);
            throw new RuntimeException(e);
        }
    }


    @Override
    public <T> T get(String index, GetResponseMapper<T> mapper, String[] sourceIncludes, String id) {
        final GetRequest request = new GetRequest(index, Constants.DEFAULT_TYPE, id);
        FetchSourceContext fetchSourceContext =
                new FetchSourceContext(true, sourceIncludes, null);
        request.fetchSourceContext(fetchSourceContext);

        log.info("GetRequest: [{}]", request.toString());
        return this.execute(client -> {
            final GetResponse response = client.get(request, RequestOptions.DEFAULT);
            return mapper.mapRow(response);
        });
    }

    @Override
    public <T> List<T> mGet(String index, MultiGetItemMapper<T> mapper, String[] sourceIncludes, String... ids) {
        final MultiGetRequest request = new MultiGetRequest();
        FetchSourceContext fetchSourceContext = new FetchSourceContext(true, sourceIncludes, null);

        for (String id : ids) {
            request.add(
                    new MultiGetRequest.Item(index, Constants.DEFAULT_TYPE, id).fetchSourceContext(fetchSourceContext)
            );
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
    public <T> List<T> ids(String index, SearchHitMapper<T> mapper, String[] sourceIncludes, String... ids) {
        final SearchRequest request = new SearchRequest(index).types(Constants.DEFAULT_TYPE);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.idsQuery().addIds(ids))
                .fetchSource(sourceIncludes, null);
        request.source(searchSourceBuilder);

        return this.execute(client -> {
            List<T> rs = new ArrayList<>();
            int rowNum = 0;
            for (SearchHit hit : client.search(request, RequestOptions.DEFAULT).getHits()) {
                rs.add(mapper.mapRow(hit, rowNum++));
            }
            return rs;
        });
    }


    @Override
    public <T> ElasticsearchPageResult<T> search(SearchHitMapper<T> mapper, SearchRequest searchRequest) {
        log.info("send request json:{}", searchRequest.toString());
        return this.execute(client -> {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            List<T> rs = new ArrayList<>();
            int rowNum = 0;
            for (SearchHit hit : searchResponse.getHits()) {
                rs.add(mapper.mapRow(hit, rowNum++));
            }

            final ElasticsearchPageResult<T> result = new ElasticsearchPageResult<>();
            result.setTotalCount(searchResponse.getHits().getTotalHits());
            result.setData(rs);
            return result;
        });
    }


    @Override
    public <T> ElasticsearchPageResult<T> searchAll(SearchHitMapper<T> mapper, int size, String... indices) {
        SearchRequest searchRequest = new SearchRequest(indices).types(Constants.DEFAULT_TYPE);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery())
                .size(size)
                .trackTotalHits(true);

        searchRequest.source(searchSourceBuilder);

        return this.search(mapper, searchRequest);
    }

    @Override
    public <T> ElasticsearchPageResult<T> searchDocs(QueryBuilder queryBuilder, SortBuilder<?>[] sort,
                                                     String[] sourceIncludes, @Nullable String[] sourceExcludes,
                                                     Integer pageNo, Integer pageSize,
                                                     SearchHitMapper<T> mapper, String... indices) {
        SearchRequest searchRequest = new SearchRequest(indices).types(Constants.DEFAULT_TYPE);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder)
                .fetchSource(sourceIncludes, sourceExcludes)
                .from((pageNo - 1) * pageSize)
                .size(pageSize)
                .trackTotalHits(true);

        for (SortBuilder<?> sortBuilder : sort) {
            searchSourceBuilder.sort(sortBuilder);
        }
        searchRequest.source(searchSourceBuilder);

        return this.search(mapper, searchRequest);
    }


    @Override
    public <T> T searchIndexAndAggs(QueryBuilder queryBuilder, SortBuilder<?>[] sort, Integer pageNo, Integer pageSize,
                                    AggregationBuilder aggregationBuilder, SearchResponseMapper<T> mapper, String... indices) {

        SearchRequest searchRequest = new SearchRequest(indices).types(Constants.DEFAULT_TYPE);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder)
                .from((pageNo - 1) * pageSize)
                .size(pageSize)
                .aggregation(aggregationBuilder);

        for (SortBuilder<?> sortBuilder : sort) {
            searchSourceBuilder.sort(sortBuilder);
        }
        searchRequest.source(searchSourceBuilder);

        log.info("send request json:{}", searchRequest.toString());
        return this.execute(client -> {
            final SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            return mapper.mapRow(searchResponse);
        });
    }


    @Override
    public <T> T searchByScroll(QueryBuilder queryBuilder, SortBuilder<?>[] sort,
                                String[] sourceIncludes, @Nullable String[] sourceExcludes,
                                SearchResponseMapper<T> mapper, String... indices) {
        SearchRequest searchRequest = new SearchRequest(indices).types(Constants.DEFAULT_TYPE);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder)
                .size(2000)
                .fetchSource(sourceIncludes, sourceExcludes);

        if (sort == null || sort.length == 0) {
            searchSourceBuilder.sort(SortBuilders.fieldSort("_doc"));
        } else {
            for (SortBuilder<?> sortBuilder : sort) {
                searchSourceBuilder.sort(sortBuilder);
            }
        }
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueSeconds(60));

        log.info("send request json:{}", searchSourceBuilder.toString());
        return this.execute(client -> {
            final SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            return mapper.mapRow(searchResponse);
        });
    }

    @Override
    public <T> T searchByScrollId(String scrollId, SearchResponseMapper<T> mapper) {
        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(TimeValue.timeValueSeconds(60));

        log.info("searchByScrollId scrollID {}", scrollId);
        return this.execute(client -> {
            final SearchResponse searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
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
    public boolean addDoc(String index, boolean create, IndexDoc... docs) {
        BulkRequest bulk = new BulkRequest(index, Constants.DEFAULT_TYPE);
        bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        for (IndexDoc doc : docs) {
            final IndexRequest request = new IndexRequest();
            if (StringUtils.isNotBlank(doc.getId())) {
                request.id(doc.getId());
            }
            request.create(create);
            request.source(doc.getJsonString(), XContentType.JSON);

            bulk.add(request);
        }

        return this.bulk(bulk);
    }


    @Override
    public boolean updateDocByScript(String index, Script script, String... ids) {
        BulkRequest bulk = new BulkRequest(index, Constants.DEFAULT_TYPE);
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
    public boolean updateDoc(String index, boolean docAsUpsert, IndexDoc... docs) {
        BulkRequest bulk = new BulkRequest(index, Constants.DEFAULT_TYPE);
        bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        for (IndexDoc doc : docs) {
            final UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.id(doc.getId())
                    .doc(doc.getJsonString(), XContentType.JSON)
                    .docAsUpsert(docAsUpsert)
                    .retryOnConflict(3);

            bulk.add(updateRequest);
        }
        return this.bulk(bulk);
    }

    @Override
    public boolean deleteDoc(String index, String... ids) {
        BulkRequest bulk = new BulkRequest(index, Constants.DEFAULT_TYPE);
        bulk.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        for (String id : ids) {
            final DeleteRequest deleteRequest = new DeleteRequest().id(id);

            bulk.add(deleteRequest);
        }
        return this.bulk(bulk);
    }


    @Override
    public boolean updateByQuery(String index, QueryBuilder queryBuilder, Script script) {
        UpdateByQueryRequest request = new UpdateByQueryRequest(index).setDocTypes(Constants.DEFAULT_TYPE);
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
        SearchRequest searchRequest = new SearchRequest(indices).types(Constants.DEFAULT_TYPE);
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


    @Override
    public void addDocAsync(String index, String jsonString) {
        final IndexRequest request = new IndexRequest(index).type(Constants.DEFAULT_TYPE)
                .source(jsonString, XContentType.JSON);
        bulkProcessor.add(request);
    }

    @Override
    public void addDocAsync(String index, String id, Map<String, Object> json) {
        final IndexRequest request = new IndexRequest(index).type(Constants.DEFAULT_TYPE)
                .id(id)
                .source(json);
        bulkProcessor.add(request);
    }

    @Override
    public void addDocAsync(String index, String id, String jsonString) {
        final IndexRequest request = new IndexRequest(index).type(Constants.DEFAULT_TYPE)
                .id(id)
                .source(jsonString, XContentType.JSON);
        bulkProcessor.add(request);
    }

    @Override
    public void deleteDocAsync(String index, String id) {
        final DeleteRequest request = new DeleteRequest(index).type(Constants.DEFAULT_TYPE).id(id);
        bulkProcessor.add(request);
    }

    @Override
    public void updateDocAsync(String index, String id, Map<String, Object> json, boolean docAsUpsert) {
        final UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(index).type(Constants.DEFAULT_TYPE)
                .id(id)
                .doc(json)
                .docAsUpsert(docAsUpsert)
                .retryOnConflict(3);
        bulkProcessor.add(updateRequest);
    }

    @Override
    public void updateDocAsync(String index, String id, String jsonString, boolean docAsUpsert) {
        final UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(index).type(Constants.DEFAULT_TYPE)
                .id(id)
                .doc(jsonString, XContentType.JSON)
                .docAsUpsert(docAsUpsert)
                .retryOnConflict(3);
        bulkProcessor.add(updateRequest);
    }


    @Override
    public void refresh(String... indices) {
        this.execute(client -> client.indices().refresh(new RefreshRequest(indices), RequestOptions.DEFAULT));
    }

    @Override
    public void close() {
        try {
            bulkProcessor.awaitClose(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void flush() {
        bulkProcessor.flush();
    }

}
