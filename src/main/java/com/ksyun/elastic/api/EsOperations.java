package com.ksyun.elastic.api;

import com.ksyun.elastic.dto.ElasticsearchPageResult;
import com.ksyun.elastic.dto.IndexDoc;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * ES操作模板接口
 *
 * @author zhaoguodong
 */
public interface EsOperations {

    /**
     * /判断索引是否存在
     *
     * @param indices 索引名称
     * @return 存在则返回true
     */
    Boolean indicesExists(String... indices) throws IOException;

    /**
     * Executes the given action against the specified table handling resource management.
     * <p>
     * Application exceptions thrown by the action object get propagated to the caller (can only be unchecked).
     * Allows for returning a result object (typically a domain object or collection of domain objects).
     *
     * @param action action type, implemented by {@link ClientCallback}
     * @return the result object of the callback action, or null
     */
    <T> T execute(ClientCallback<T> action);

    /**
     * GET
     *
     * @param index  index名称
     * @param mapper 映射器
     * @param id     id
     * @return 结果
     */
    <T> T get(String index, GetResponseMapper<T> mapper, String[] sourceIncludes, String id);

    /**
     * 批量GET
     *
     * @param index  index名称
     * @param mapper 映射器
     * @param sourceIncludes 需要返回的字段
     * @param ids    id列表
     * @return 结果
     */
    <T> List<T> mget(String index, MultiGetItemMapper<T> mapper, String[] sourceIncludes, String... ids);

    /**
     * 批量根据ID检索
     *
     * @param index  index名称
     * @param mapper 映射器
     * @param sourceIncludes 需要返回的字段
     * @param ids    id列表
     * @return 结果
     */
    <T> List<T> ids(String index, SearchHitMapper<T> mapper, String[] sourceIncludes, String... ids);

    /**
     * 检索通用方法
     *
     * @param mapper        映射器
     * @param searchRequest 检索请求
     * @return 检索结果
     */
    <T> ElasticsearchPageResult<T> search(SearchHitMapper<T> mapper, SearchRequest searchRequest);

    /**
     * 检索全部
     *
     * @param mapper  映射器
     * @param size    返回条数
     * @param indices index名称
     * @return 检索结果
     */
    <T> ElasticsearchPageResult<T> searchAll(SearchHitMapper<T> mapper, int size, String... indices);

    /**
     * 条件检索
     *
     * @param queryBuilder   查询条件
     * @param sort           排序
     * @param sourceIncludes 需要返回的字段
     * @param sourceExcludes 需要排除的字段
     * @param pageNo         当前页
     * @param pageSize       每页条数
     * @param mapper         映射器
     * @param indices        index名称
     * @return 检索结果
     */
    <T> ElasticsearchPageResult<T> searchDocs(QueryBuilder queryBuilder, SortBuilder<?>[] sort,
                                              String[] sourceIncludes, String[] sourceExcludes,
                                              Integer pageNo, Integer pageSize,
                                              SearchHitMapper<T> mapper, String... indices);

    /**
     * 条件检索并聚合
     *
     * @param queryBuilder       查询条件
     * @param sort               排序
     * @param pageNo             当前页
     * @param pageSize           每页条数
     * @param aggregationBuilder 聚合条件
     * @param mapper             映射器
     * @param indices            index名称
     * @return 检索结果
     */
    <T> T searchIndexAndAggs(QueryBuilder queryBuilder, SortBuilder<?>[] sort, Integer pageNo, Integer pageSize,
                             AggregationBuilder aggregationBuilder, SearchResponseMapper<T> mapper, String... indices);

    /**
     * 滚动查询
     *
     * @param queryBuilder   查询条件
     * @param sort           排序
     * @param sourceIncludes 需要返回的字段
     * @param sourceExcludes 需要排除的字段
     * @param mapper         映射器
     * @param indices        索引名称
     * @return 结果
     */
    <T> T searchByScroll(QueryBuilder queryBuilder, SortBuilder<?>[] sort, String[] sourceIncludes, String[] sourceExcludes, SearchResponseMapper<T> mapper, String... indices);

    /**
     * 根据滚动ID获取数据
     *
     * @param scrollId 滚动ID
     * @param mapper   映射器
     * @return 单次滚动结果
     */
    <T> T searchByScrollId(String scrollId, SearchResponseMapper<T> mapper);

    /**
     * 批量处理文档
     *
     * @param bulk 批量请求
     * @return 成功返回true
     */
    boolean bulk(BulkRequest bulk);

    /**
     * 批量新增文档
     *
     * @param index  索引名称
     * @param create id存在时，是否跳过
     * @param docs   需要新增的json字符串列表
     * @return 新增结果
     */
    boolean addDoc(String index, boolean create, IndexDoc... docs);

    /**
     * 使用脚本更新文档
     *
     * @param index  索引名称
     * @param script 脚本
     * @param ids    ids
     * @return 修改结果
     */
    boolean updateDocByScript(String index, Script script, String... ids);

    /**
     * 批量修改文档
     *
     * @param index       索引名称
     * @param docAsUpsert id不存在时，是否新增
     * @param docs        需要修改的json字符串列表
     * @return 修改结果
     */
    boolean updateDoc(String index, boolean docAsUpsert, IndexDoc... docs);

    /**
     * 批量删除文档
     *
     * @param index 索引名称
     * @param ids   id列表
     * @return 结果
     */
    boolean deleteDoc(String index, String... ids);

    /**
     * 通过查询更新
     *
     * @param index        索引名称
     * @param queryBuilder 查询条件
     * @param script       更新脚本
     * @return 更新结果信息
     */
    boolean updateByQuery(String index, QueryBuilder queryBuilder, Script script);

    /**
     * 异步新增，加入索引请求到缓冲池
     *
     * @param index      索引名称
     * @param jsonString 索引实体
     */
    void addDocAsync(String index, String jsonString);


    /**
     * 异步新增，加入索引请求到缓冲池
     *
     * @param index 索引名称
     * @param id    ID
     * @param json  实体
     */
    void addDocAsync(String index, String id, Map<String, Object> json);

    /**
     * 异步新增，加入索引请求到缓冲池
     *
     * @param index      索引名称
     * @param id         ID
     * @param jsonString 索引实体
     */
    void addDocAsync(String index, String id, String jsonString);

    /**
     * 异步删除，加入删除请求到缓冲池
     *
     * @param index 索引名称
     * @param id    ID
     */
    void deleteDocAsync(String index, String id);

    /**
     * 异步更新，加入更新请求到缓冲池
     *
     * @param index       索引名
     * @param id          id
     * @param json        更新请求体
     * @param docAsUpsert 不存在时是否新增
     */
    void updateDocAsync(String index, String id, Map<String, Object> json, boolean docAsUpsert);

    /**
     * 异步更新，加入更新请求到缓冲池
     *
     * @param index       索引名
     * @param id          id
     * @param jsonString  更新请求体
     * @param docAsUpsert 不存在时是否新增
     */
    void updateDocAsync(String index, String id, String jsonString, boolean docAsUpsert);

    /**
     * 手动刷新索引
     *
     * @param indices 索引名称
     */
    void refresh(String... indices);

    /**
     * 关闭连接
     */
    void close();

    /**
     * 手动刷新批处理器
     */
    void flush();
}