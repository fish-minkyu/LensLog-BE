package com.example.LensLog.search.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// OpenSearch에 넣을 문서 DTO
@Data
@Builder
public class SearchReqDto {
    private Long photoId;
    private String imageUrl;

    private String caption; // OpenAI가 생성
    private List<String> tags; // OpenAI가 생성

    private String location;
    private Long categoryId;
    private String categoryName;

    private LocalDate shotDate;
    private float[] mmVector;
}
