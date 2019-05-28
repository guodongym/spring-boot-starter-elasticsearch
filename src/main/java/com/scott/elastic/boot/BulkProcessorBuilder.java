/*
 * Copyright 2017 Cognitree Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.scott.elastic.boot;

import com.scott.elastic.config.ElasticSearchConfig;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import java.util.function.BiConsumer;

/**
 * This class creates  an instance of the {@link BulkProcessor}
 *
 * @author zhaogd
 */
@Slf4j
public class BulkProcessorBuilder {


    public static BulkProcessor build(final RestHighLevelClient client, final ElasticSearchConfig config) {
        log.info("Bulk processor bulkActions: [{}], bulkSize: [{}], flush interval time: [{}]," +
                        " concurrent Request: [{}], backoffPolicyTimeInterval: [{}], backoffPolicyRetries: [{}] ",
                config.getBulkActions(), config.getBulkSize(), config.getFlushIntervalTime(),
                config.getConcurrentRequests(), config.getExponentialBackoffPolicyInitialDelay(),
                config.getExponentialBackoffPolicyRetries());

        // 构建异步客户端
        BiConsumer<BulkRequest, ActionListener<BulkResponse>> bulkConsumer =
                (request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener);

        // 构建批量处理器
        return BulkProcessor.builder(bulkConsumer, getListener())
                .setBulkActions(config.getBulkActions())
                .setBulkSize(new ByteSizeValue(config.getBulkSize(), config.getBulkSizeUnit()))
                .setFlushInterval(new TimeValue(config.getFlushIntervalTime(), config.getFlushIntervalTimeUnit()))
                .setConcurrentRequests(config.getConcurrentRequests())
                .setBackoffPolicy(
                        BackoffPolicy.exponentialBackoff(
                                new TimeValue(config.getExponentialBackoffPolicyInitialDelay(),
                                        config.getExponentialBackoffPolicyInitialDelayUnit()),
                                config.getExponentialBackoffPolicyRetries()
                        )
                )
                .build();
    }

    private static BulkProcessor.Listener getListener() {
        return new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
                int numberOfActions = request.numberOfActions();
                log.info("Executing bulk [{}] with {} requests",
                        executionId, numberOfActions);
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                if (response.hasFailures()) {
                    log.warn("Bulk [{}] executed with failures, Failures Message: {}",
                            executionId, response.buildFailureMessage());
                } else {
                    log.info("Bulk [{}] completed in {} milliseconds, Count [{}]",
                            executionId, response.getTook().getMillis(), response.getItems().length);
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("Unable to send request to elasticsearch.", failure);
            }
        };
    }

}
