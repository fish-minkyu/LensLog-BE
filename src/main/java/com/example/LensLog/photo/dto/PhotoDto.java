package com.example.LensLog.photo.dto;

import com.example.LensLog.category.dto.CategoryDto;
import com.example.LensLog.photo.entity.Photo;
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

    public static PhotoDto fromEntity(Photo entity) {
        return PhotoDto.builder()
            .photoId(entity.getPhotoId())
            .fileName(entity.getFileName())
            .location(entity.getLocation())
            .shotDate(entity.getShotDate())
            .bucketFileUrl(entity.getBucketFileUrl())
            .thumbnailUrl(entity.getThumbnailUrl())
            .views(entity.getViews())
            .downloads(entity.getDownloads())
            .likeCount(entity.getGoods().size())
            .build();
    }
}
