package com.scott.elastic.auto;

import com.scott.elastic.service.ISearchService;
import com.scott.elastic.service.impl.SearchServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsAutoConfiguration {

    @Bean
    ISearchService iSearchService() {
        return new SearchServiceImpl();
    }

}
