package com.scott.elastic.config;

import com.scott.elastic.auto.EsProperties;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
@Configuration
@EnableConfigurationProperties(EsProperties.class)
public class ElasticSearchConfig {

    @Autowired
    private EsProperties esProperties;

    @Bean
    public TransportClient esTransportClient() throws UnknownHostException {

        Settings settings = Settings.builder()
                .put( "cluster.name", esProperties.getClusterName() )
                .put("client.transport.sniff", true)
                .build();
        TransportAddress master = new TransportAddress( InetAddress.getByName( esProperties.getHost() ), esProperties.getTcpPort() );
		TransportClient esClient = new PreBuiltTransportClient(settings).addTransportAddress( master );
        return esClient;
    }

    @Bean
    public RestHighLevelClient esRestHighLevelClient() {

        RestHighLevelClient restHighLevelClient = null;
        if( null!=esProperties.getAuth() && "true".equals( esProperties.getAuth().get("enable")) ) {

            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials( AuthScope.ANY,new UsernamePasswordCredentials( esProperties.getAuth().get("username"), esProperties.getAuth().get("password") ));

            RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost( esProperties.getHost(), esProperties.getHttpPort() ))
                    .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                        @Override
                        public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                            return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                        }
                    });

            restHighLevelClient = new RestHighLevelClient(restClientBuilder);
            return restHighLevelClient ;
        }

        restHighLevelClient = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost( esProperties.getHost(), esProperties.getHttpPort(), "http" )
                )
        );
        return restHighLevelClient;
    }
}
