package com.scott.elastic.api;

import com.scott.elastic.dto.IndexDoc;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import java.io.IOException;
import java.util.List;

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
     * 批量GET
     *
     * @param index  index名称
     * @param mapper 映射器
     * @param ids    id列表
     * @return 结果
     */
    <T> List<T> get(String index, MultiGetItemMapper<T> mapper, String... ids);

    /**
     * 检索通用方法
     *
     * @param mapper        映射器
     * @param searchRequest 检索请求
     * @return 检索结果
     */
    <T> List<T> search(SearchHitMapper<T> mapper, SearchRequest searchRequest);

    /**
     * 检索全部
     *
     * @param mapper  映射器
     * @param size    返回条数
     * @param indices index名称
     * @return 检索结果
     */
    <T> List<T> searchAll(SearchHitMapper<T> mapper, int size, String... indices);

    /**
     * 条件检索
     *
     * @param queryBuilder   查询条件
     * @param sort           排序
     * @param sourceIncludes 需要返回的字段
     * @param pageNo         当前页
     * @param pageSize       每页条数
     * @param mapper         映射器
     * @param indices        index名称
     * @return 检索结果
     */
    <T> List<T> searchDocs(QueryBuilder queryBuilder, SortBuilder sort, String[] sourceIncludes, Integer pageNo, Integer pageSize,
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
    <T> T searchIndexAndAggs(QueryBuilder queryBuilder, SortBuilder sort, Integer pageNo, Integer pageSize,
                             AggregationBuilder aggregationBuilder, SearchResponseMapper<T> mapper, String... indices);

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
     * @param index 索引名称
     * @param docs  需要新增的json字符串列表
     * @return 新增结果
     */
    boolean addDoc(String index, IndexDoc... docs);

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
     * @param index 索引名称
     * @param docs  需要修改的json字符串列表
     * @return 修改结果
     */
    boolean updateDoc(String index, IndexDoc... docs);

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
}