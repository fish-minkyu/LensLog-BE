package com.example.LensLog.search.indexing;

import com.example.LensLog.search.indexing.error.EmbedImageUrlException;
import com.example.LensLog.search.indexing.error.IndexingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

// Python CLIP 서버 호출용 RestTemplate
// : CLIP 임베딩은 이미지 분석해서 vector 값 계산
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final RestTemplate restTemplate;

    @Value("${embedding.baseUrl}")
    private String baseUrl;

    public float[] embedText(String text) {
        String url = baseUrl + "/embed/text";
        TextReq req = new TextReq(text);
        EmbedRes res = post(url, req);
        validate(res, "text");

        List<Float> vectorList = res.getVector();
        float[] vectorArray = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            vectorArray[i] = vectorList.get(i);
        }

        return vectorArray;
    }

    public float[] embedImageUrl(Long photoId, String imageUrl) {
        try {
            String url = baseUrl + "/embed/image";
            ImageReq req = new ImageReq(imageUrl);
            EmbedRes res = post(url, req);
            validate(res, "image");

            List<Float> vectorList = res.getVector();
            float[] vectorArray = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                vectorArray[i] = vectorList.get(i);
            }

            return vectorArray;
        } catch (Exception e) {
            throw new EmbedImageUrlException(photoId, "Saving OpenSearch doc error", e);
        }
    }

    private EmbedRes post(String url, Object body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            ResponseEntity<EmbedRes> resp = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                EmbedRes.class
            );

            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Embedding server returned non-2xx: " + resp.getStatusCode());
            }

            return resp.getBody();
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to call embedding server: " + url + " (" + e.getMessage() + ")", e);
        }
    }

    private void validate(EmbedRes res, String type) {
        if (res == null || res.getVector() == null || res.getVector().isEmpty()) {
            throw new IllegalStateException("Embedding response is empty (" + type + ")");
        }
        // OpenCLIP ViT-B-32 기준 512차원. 나중에 모델 바꾸면 이 값만 조절.
        if (res.getDim() != 512) {
            throw new IllegalStateException("Unexpected embedding dim: " + res.getDim() + " (expected 512)");
        }
    }

    // ===== DTOs =====
    public static class TextReq {
        private String text;
        public TextReq() {}
        public TextReq(String text) { this.text = text; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class ImageReq {
        private String image_url;
        public ImageReq() {}
        public ImageReq(String imageUrl) { this.image_url = imageUrl; }
        public String getImage_url() { return image_url; }
        public void setImage_url(String image_url) { this.image_url = image_url; }
    }

    public static class EmbedRes {
        private int dim;
        private List<Float> vector;

        public EmbedRes() {}
        public int getDim() { return dim; }
        public void setDim(int dim) { this.dim = dim; }
        public List<Float> getVector() { return vector; }
        public void setVector(List<Float> vector) { this.vector = vector; }
    }
}