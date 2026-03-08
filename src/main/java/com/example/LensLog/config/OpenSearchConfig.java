package com.example.LensLog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    @Bean(destroyMethod = "close")
    public RestClient opensearchRestClient(
        @Value("${opensearch.host}") String host,
        @Value("${opensearch.port}") int port,
        @Value("${opensearch.scheme}") String scheme,
        @Value("${opensearch.username}") String username,
        @Value("${opensearch.password}") String password
    ) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        return RestClient.builder(new HttpHost(host, port, scheme))
            .setHttpClientConfigCallback(httpAsyncClientBuilder ->
                httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            )
            .build();
    }

    @Bean
    public OpenSearchClient openSearchClient(RestClient restClient) {
        ObjectMapper om = new ObjectMapper();
        // 날짜, 시간 직렬화 지원
        om.registerModule(new JavaTimeModule());
        // 다른 모듈도 자동 등록(있으면 자동 등록)
        om.findAndRegisterModules();

        JacksonJsonpMapper mapper = new JacksonJsonpMapper(om);
        RestClientTransport transport = new RestClientTransport(restClient, mapper);

        return new OpenSearchClient(transport);
    }
}
