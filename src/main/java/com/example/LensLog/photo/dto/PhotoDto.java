package com.example.LensLog.photo.dto;

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
    private Long views;
    private Long downloads;
    private int likeCount; // 좋아요 수

    public static PhotoDto fromEntity(Photo entity) {
        return PhotoDto.builder()
            .photoId(entity.getPhotoId())
            .fileName(entity.getFileName())
            .location(entity.getLocation())
            .shotDate(entity.getShotDate())
            .bucketFileUrl(entity.getBucketFileUrl())
            .views(entity.getViews())
            .downloads(entity.getDownloads())
            .likeCount(entity.getGoods().size())
            .build();
    }
}
