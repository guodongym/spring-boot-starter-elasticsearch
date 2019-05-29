package com.scott.elastic.boot;

import com.google.common.base.Preconditions;
import com.scott.elastic.api.EsTemplate;
import com.scott.elastic.config.ElasticSearchConfig;
import com.scott.elastic.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * es自动配置类
 *
 * @author zhaogd
 * @date 2019/5/28
 */
@Configuration
@EnableConfigurationProperties(ElasticSearchConfig.class)
@ConditionalOnClass(EsTemplate.class)
@Slf4j
public class ElasticSearchAutoConfiguration {

    private final ElasticSearchConfig config;

    public ElasticSearchAutoConfiguration(ElasticSearchConfig config) {
        this.config = config;
    }


    @Bean
    @ConditionalOnMissingBean(EsTemplate.class)
    public EsTemplate esTemplate() {
        final String hostString = config.getHosts();
        Preconditions.checkNotNull(hostString,
                "spring.data.es.hosts cannot be empty, please specify in configuration file");

        String[] hosts = hostString.split(Constants.COMMA);
        Preconditions.checkArgument(ArrayUtils.isNotEmpty(hosts),
                "spring.data.es.hosts cannot be empty, please specify in configuration file");

        final RestHighLevelClient client = buildClient(hosts);
        return new EsTemplate(client, BulkProcessorBuilder.build(client, config));
    }


    private List<HttpHost> getHttpHosts(String[] hosts) {
        List<HttpHost> httpHosts = new ArrayList<>(hosts.length);
        for (String hostName : hosts) {
            String[] hostDetails = hostName.split(Constants.COLONS);
            String address = hostDetails[0];
            int port = hostDetails.length > 1 ? Integer.parseInt(hostDetails[1]) : Constants.DEFAULT_ES_PORT;
            httpHosts.add(new HttpHost(address, port));
        }
        return httpHosts;
    }

    private RestHighLevelClient buildClient(String[] hosts) {
        final List<HttpHost> httpHosts = getHttpHosts(hosts);
        log.info("HostName: [{}] ", httpHosts);
        return new RestHighLevelClient(RestClient.builder(httpHosts.toArray(new HttpHost[]{})));
    }


}
