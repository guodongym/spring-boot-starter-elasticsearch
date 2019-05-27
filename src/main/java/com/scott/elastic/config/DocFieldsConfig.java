package com.scott.elastic.config;

import com.scott.elastic.auto.EsProperties;
import com.scott.elastic.dto.DocModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Configuration
@EnableConfigurationProperties(EsProperties.class)
public class DocFieldsConfig {

    @Autowired
    private EsProperties esProperties;

    @Bean
    public DocModel docModel() {
        List<String> docFields = new ArrayList<>();
        String[] fields = esProperties.getDocFields().split(",");
        for( int i=0; i<fields.length; ++i )
            docFields.add( fields[i] );
        return new DocModel( docFields );
    }

}
