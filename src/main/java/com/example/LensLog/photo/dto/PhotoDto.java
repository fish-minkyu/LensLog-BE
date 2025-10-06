package com.example.LensLog.photo.dto;

import com.example.LensLog.photo.entity.Photo;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhotoDto {
    private Long photoId;
    private String fileName;
    private String bucketFileUrl;
    private Long views;
    private Long downloads;
    private int likeCount; // 좋아요 수

    public static PhotoDto fromEntity(Photo entity) {
        return PhotoDto.builder()
            .photoId(entity.getPhotoId())
            .fileName(entity.getFileName())
            .bucketFileUrl(entity.getBucketFileUrl())
            .views(entity.getViews())
            .downloads(entity.getDownloads())
            .likeCount(entity.getLikes().size())
            .build();
    }
}
