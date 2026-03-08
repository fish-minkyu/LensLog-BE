package com.example.LensLog.search.indexing.service;

import com.example.LensLog.search.dto.TaggingResult;
import com.example.LensLog.search.indexing.error.AiTaggingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.*;
import java.util.Base64;
import java.util.List;

// C. OpenAI 태깅 서비스
// : AI 태깅 전담 서비스
// - OpenAI 호출, 이미지 bytes를 보내서 caption + tags 생성
@Service
@RequiredArgsConstructor
public class AITaggingService {
    private final RestTemplate restTemplate;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${openai.apiKey}")
    private String apiKey;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    public TaggingResult generateTagsAndCaption(Long photoId, byte[] imageBytes) {
        String dataUrl
            = "data:image/webp;base64," + Base64.getEncoder().encodeToString(imageBytes);
        // Responses API payload (JSON만 출력하도록 강하게 지시)
        String payload = """
        {
          "model": "%s",
          "input": [
            {
              "role": "user",
              "content": [
                {
                  "type": "input_text",
                  "text": "너는 이미지 검색용 태그 생성기야. 아래 이미지에서 검색에 도움되는 tags 15개(한국어 우선, 필요시 영어 병기)와 caption 1문장을 JSON으로만 출력해. 형식: {\\\"caption\\\":\\\"...\\\",\\\"tags\\\":[\\\"...\\\",...]} 다른 문장/설명 금지."
                },
                {
                  "type": "input_image",
                  "image_url": "%s"
                }
              ]
            }
          ]
        }
        """.formatted(model, dataUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> req = new HttpEntity<>(payload, headers);

        String body;
        try {
            ResponseEntity<String> res = restTemplate.exchange(
                "https://api.openai.com/v1/responses",
                HttpMethod.POST,
                req,
                String.class
            );

            body = res.getBody();
            if (body == null || body.isBlank()) {
                throw new AiTaggingException(
                    photoId,
                    AiTaggingException.AiErrorReason.OPENAI_EMPTY_BODY,
                    "OpenAI response body is empty");
            }
        } catch (HttpStatusCodeException e) {
            // 4xx, 5xx 에러
            String resBody = e.getResponseBodyAsString();
            throw new AiTaggingException(
                photoId,
                AiTaggingException.AiErrorReason.OPENAI_NON_2XX,
                "OpenAI returned " + e.getStatusCode() + " body=" + resBody,
                e
            );
        } catch (ResourceAccessException e) {
            // 타임아웃, DNS, 연결 문제
            throw new AiTaggingException(
                photoId,
                AiTaggingException.AiErrorReason.OPENAI_HTTP_ERROR,
                "OpenAI network error: " + e.getMessage(),
                e);
        } catch (Exception e) {
            throw new AiTaggingException(
                photoId,
                AiTaggingException.AiErrorReason.OPENAI_HTTP_ERROR,
                "OpenAI request failed: " + e.getMessage(),
                e);
        }

        // 파싱 작업 시작
        try {
            JsonNode root = om.readTree(body);
            String outText = root.path("output_text").asText(null);
            if (outText == null || outText.isBlank()) {
                outText = findFirstText(root);
            }

            JsonNode parsed = om.readTree(outText);
            String caption = parsed.path("caption").asText("");
            List<String> tags = om.convertValue(
                parsed.path("tags"),
                om.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            return new TaggingResult(caption, tags == null ? List.of() : tags);
        } catch (Exception e) {
            throw new AiTaggingException(
                photoId,
                AiTaggingException.AiErrorReason.OPENAI_PARSE_ERROR,
                "OpenAI response parse failed",
                e);
        }
    }

    private String findFirstText(JsonNode root) {
        // output[0].content[0].text 같은 구조를 대충 훑어서 text를 찾는 보수적 처리
        JsonNode output = root.path("output");
        if (output.isArray() && output.size() > 0) {
            JsonNode content = output.get(0).path("content");

            if (content.isArray() && content.size() > 0) {
                JsonNode text = content.get(0).path("text");
                if (!text.isMissingNode()) return text.asText("");
            }
        }

        return "";
    }
}
