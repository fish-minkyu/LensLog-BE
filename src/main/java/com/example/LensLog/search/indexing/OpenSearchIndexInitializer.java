package com.example.LensLog.search.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// 앱 시작 시 OpenSearch 인덱스 및 매핑 보장
// : 운영 자동화에 도움
// - knn = true
// - mmVector = knn_vector
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenSearchIndexInitializer implements ApplicationRunner {

    private final RestClient restClient;

    @Value("${opensearch.initIndex:true}")
    private boolean initIndex;

    @Value("${opensearch.indexVersioned:lenslog-photos-v1}")
    private String indexVersioned;

    @Value("${opensearch.indexAlias:lenslog-photos}")
    private String indexAlias;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!initIndex) return;

        if (!indexExists(indexVersioned)) {
            createIndexWithKnn(indexVersioned);
            log.info("Created index {}", indexVersioned);
        } else {
            log.info("Index already exists {}", indexVersioned);
        }

        ensureAlias(indexAlias, indexVersioned);
        log.info("Alias {} -> {}", indexAlias, indexVersioned);
    }

    private boolean indexExists(String index) throws Exception {
        Request req = new Request("HEAD", "/" + index);
        Response resp = restClient.performRequest(req);
        int code = resp.getStatusLine().getStatusCode();
        return code == 200;
    }

    private void createIndexWithKnn(String index) throws Exception {
        String body = """
        {
          "settings": {
            "index": {
              "knn": true,
              "number_of_shards": 1,
              "number_of_replicas": 0
            }
          },
          "mappings": {
            "properties": {
              "photoId": { "type": "long" },
              "imageUrl": { "type": "keyword" },
              "caption": { "type": "text" },
              "tags": { "type": "keyword" },
              "location": { "type": "text" },
              "categoryId": { "type": "long", "null_value": -1 },
              "categoryName": { "type": "keyword" },
              "shotDate": { "type": "date" },
              "createdAt": { "type": "date" },
              "mmVector": {
                "type": "knn_vector",
                "dimension": 512,
                "method": {
                  "name": "hnsw",
                  "engine": "nmslib",
                  "space_type": "cosinesimil"
                }
              }
            }
          }
        }
        """;

        Request req = new Request("PUT", "/" + index);
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        restClient.performRequest(req);
    }

    private void ensureAlias(String alias, String index) throws Exception {
        // alias가 이미 다른 인덱스를 가리키고 있을 수도 있으니,
        // 일단 "add"만 수행(개발/MVP)
        String body = """
        {
          "actions": [
            { "add": { "index": "%s", "alias": "%s" } }
          ]
        }
        """.formatted(index, alias);

        Request req = new Request("POST", "/_aliases");
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        restClient.performRequest(req);
    }
}
