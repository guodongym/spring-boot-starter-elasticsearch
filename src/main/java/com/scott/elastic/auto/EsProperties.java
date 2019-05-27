package com.scott.elastic.auto;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@ConfigurationProperties("elasticsearch")
public class EsProperties {

    @NotBlank
    private String host;

    private int tcpPort;

    @NotBlank
    private int httpPort;

    private String clusterName;

    private String docFields;

    private Map<String,String> auth; // 是否需要x-pack权限认证 的控制属性


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getDocFields() {
        return docFields;
    }

    public void setDocFields(String docFields) {
        this.docFields = docFields;
    }

    public Map<String, String> getAuth() {
        return auth;
    }

    public void setAuth(Map<String, String> auth) {
        this.auth = auth;
    }
}
