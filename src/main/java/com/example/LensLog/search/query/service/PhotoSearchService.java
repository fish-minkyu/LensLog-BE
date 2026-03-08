package com.example.LensLog.search.query.service;

import com.example.LensLog.photo.dto.PhotoDto;
import com.example.LensLog.photo.entity.Photo;
import com.example.LensLog.photo.repo.PhotoRepository;
import com.example.LensLog.search.indexing.EmbeddingClient;
import com.example.LensLog.search.dto.SearchResDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;
import java.util.stream.Collectors;

// 검색용 오케스트레이션
// : 검색 결과를 최종 사용자 응답 형태로 만드는 서비스
// - text -> 임베딩
// - OpenSearch 검색
// - DB 재조회
// - PhotoDto + score 조합
@Service
@RequiredArgsConstructor
public class PhotoSearchService {

    private final EmbeddingClient embeddingClient;
    private final PhotoVectorSearcher vectorSearchService;
    private final PhotoRepository photoRepository;
    private final ObjectMapper objectMapper; // Spring ObjectMapper Bean 주입 추천

    public List<PhotoDto> search(String query, int size) throws Exception {
        float[] qVec = embeddingClient.embedText(query);

        int candidatesSize = Math.max(size * 20, 300);
        List<SearchResDto> candidates = vectorSearchService.knnSearch(qVec, candidatesSize);

        List<Long> ids = candidates.stream().map(SearchResDto::photoId).toList();
        Iterable<Photo> photosIt = photoRepository.findAllById(ids);

        Map<Long, Photo> photoMap = new HashMap<>();
        for (Photo p : photosIt) {
            photoMap.put(p.getPhotoId(), p);
        }

        List<String> tokens = tokenize(query);

        List<Reranked> reranked = new ArrayList<>();
        for (SearchResDto c : candidates) {
            Photo p = photoMap.get(c.photoId());
            if (p == null) continue;

            double bonus = calcTextBonus(p, tokens);
            double finalScore = c.score() + bonus;

            reranked.add(new Reranked(p, finalScore));
        }

        reranked.sort(Comparator.comparingDouble(Reranked::score).reversed());

        List<PhotoDto> result = new ArrayList<>();
        for (int i = 0; i < Math.min(size, reranked.size()); i++) {
            Reranked r = reranked.get(i);
            Photo p = r.photo();

            result.add(PhotoDto.builder()
                .photoId(p.getPhotoId())
                .fileName(p.getFileName())
                .shotDate(p.getShotDate())
                .bucketFileUrl(p.getBucketFileUrl())
                .thumbnailUrl(p.getThumbnailUrl())
                .score(r.score()) // 최종 점수(벡터 + 텍스트 보너스)
                .build());
        }

        return result;
    }

    private List<String> tokenize(String query) {
        if (query == null) return List.of();
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) return List.of();

        List<String> parts = Arrays.stream(q.split("\\s+"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toCollection(ArrayList::new));

        // "바다"처럼 한 단어 검색 대비: 전체 문자열도 토큰으로 포함
        if (!parts.contains(q)) parts.add(q);

        return parts;
    }

    private double calcTextBonus(Photo photo, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return 0.0;

        String caption = safeLower(photo.getAiCaption());
        List<String> tags = parseTags(photo.getAiTags()).stream()
            .map(this::safeLower)
            .toList();

        double bonus = 0.0;

        for (String t : tokens) {
            if (t.isBlank()) continue;

            // 캡션 히트(약)
            if (!caption.isBlank() && caption.contains(t)) {
                bonus += 0.20;
            }

            // 태그 히트(강)
            for (String tag : tags) {
                if (tag.isBlank()) continue;

                if (tag.equals(t)) {      // 정확히 일치
                    bonus += 0.35;
                    break;
                }
                if (tag.contains(t)) {    // 포함
                    bonus += 0.25;
                    break;
                }
            }
        }

        // 과도한 가산 방지
        return Math.min(bonus, 1.5);
    }

    private String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    // aiTags가 JSON 배열 문자열(["바다","파도"])일 수도, CSV("바다,파도")일 수도 있으니 둘 다 대응
    private List<String> parseTags(String aiTagsRaw) {
        if (aiTagsRaw == null) return List.of();
        String raw = aiTagsRaw.trim();
        if (raw.isBlank()) return List.of();

        // JSON 배열로 보이면 먼저 파싱
        if (raw.startsWith("[") && raw.endsWith("]")) {
            try {
                return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            } catch (Exception ignore) {
                // 실패 시 CSV fallback
            }
        }

        // CSV fallback
        String[] parts = raw.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isBlank()) out.add(t);
        }
        return out;
    }

    private record Reranked(Photo photo, double score) {}
}
