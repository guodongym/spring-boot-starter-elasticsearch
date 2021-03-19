package com.ksyun.elastic.config;

import lombok.Data;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

/**
 * es配置属性
 *
 * @author zhaogd
 */
@Data
@ConfigurationProperties(prefix = "spring.data.es")
public class ElasticSearchConfig {

    /**
     * ElasticSearch的host,多个节点逗号分隔，ip和port冒号隔开
     */
    private String hosts;

    /**
     * 写入每个批次条数
     */
    private int bulkActions = 10000;

    /**
     * 写入每个批次大小
     */
    private int bulkSize = 5;
    private ByteSizeUnit bulkSizeUnit = ByteSizeUnit.MB;

    /**
     * 写入刷新阈值
     */
    private int flushIntervalTime = 10;
    private TimeUnit flushIntervalTimeUnit = TimeUnit.SECONDS;

    /**
     * 并行度
     */
    private int concurrentRequests = 1;

    /**
     * 指数补偿策略初始时间阈值
     */
    private int exponentialBackoffPolicyInitialDelay = 50;
    private TimeUnit exponentialBackoffPolicyInitialDelayUnit = TimeUnit.MILLISECONDS;

    /**
     * 重试次数
     */
    private int exponentialBackoffPolicyRetries = 8;
}
