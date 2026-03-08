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

    @Value("${gemini.apiKey}")
    private String apiKey;

    @Value("${gemini.model:gemini-3-flash-preview}")
    private String model;

    public TaggingResult generateTagsAndCaption(Long photoId, byte[] imageBytes) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String payload = """
        {
          "contents": [
            {
              "parts": [
                {
                  "text": "너는 이미지 검색용 태그 생성기야. 아래 이미지에서 검색에 도움되는 tags 15개(한국어 우선, 필요시 영어 병기)와 caption 1문장을 JSON으로만 출력해."
                },
                {
                  "inline_data": {
                    "mime_type": "image/webp",
                    "data": "%s"
                  }
                }
              ]
            }
          ],
          "generationConfig": {
            "responseMimeType": "application/json",
            "responseJsonSchema": {
              "type": "object",
              "properties": {
                "caption": {
                  "type": "string",
                  "description": "이미지를 한 문장으로 설명하는 한국어 caption"
                },
                "tags": {
                  "type": "array",
                  "description": "검색용 태그 15개",
                  "items": {
                    "type": "string"
                  },
                  "minItems": 15,
                  "maxItems": 15
                }
              },
              "required": ["caption", "tags"]
            }
          }
        }
        """.formatted(base64Image);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<String> req = new HttpEntity<>(payload, headers);

        String body;
        try {
            ResponseEntity<String> res = restTemplate.exchange(
                "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent",
                HttpMethod.POST,
                req,
                String.class
            );

            body = res.getBody();
            if (body == null || body.isBlank()) {
                throw new AiTaggingException(
                    photoId,
                    AiTaggingException.AiErrorReason.EMPTY_BODY,
                    "Gemini response body is empty"
                );
            }
        } catch (HttpStatusCodeException e) {
            String resBody = e.getResponseBodyAsString();
            throw new AiTaggingException(
                photoId,
                AiTaggingException.AiErrorReason.NON_2XX,
                "Gemini returned " + e.getStatusCode() + " body=" + resBody,
                e
            );
        } catch (ResourceAccessException e) {
            throw new AiTaggingException(
                photoId,
                AiTaggingException.AiErrorReason.HTTP_ERROR,
                "Gemini network error: " + e.getMessage(),
                e
            );
        } catch (Exception e) {
            throw new AiTaggingException(
                photoId,
                AiTaggingException.AiErrorReason.HTTP_ERROR,
                "Gemini request failed: " + e.getMessage(),
                e
            );
        }

        try {
            JsonNode root = om.readTree(body);
            String outText = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText(null);

            if (outText == null || outText.isBlank()) {
                throw new AiTaggingException(
                    photoId,
                    AiTaggingException.AiErrorReason.PARSE_ERROR,
                    "Gemini text response is empty"
                );
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
                AiTaggingException.AiErrorReason.PARSE_ERROR,
                "Gemini response parse failed",
                e
            );
        }
    }
}
