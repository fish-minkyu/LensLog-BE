package com.example.LensLog.photo.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhotoDto {
    private Long photoId;
    private String fileName;
    private String location;
    private LocalDate shotDate;
    private String bucketFileUrl;
    private String thumbnailUrl;
    private Long views;
    private Long downloads;
    private Boolean isLiked; // 사용자가 좋아요를 했는지 여부 - true, 좋아요를 했다, false: 좋아요를 안했다.
    private int likeCount; // 좋아요 수
    private Long categoryId;
}
