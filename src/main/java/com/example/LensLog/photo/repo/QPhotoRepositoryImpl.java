package com.example.LensLog.photo.repo;

import com.example.LensLog.photo.entity.Photo;
import com.example.LensLog.photo.entity.QPhoto;
import com.example.LensLog.photo.entity.ThumbnailStatusEnum;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class QPhotoRepositoryImpl implements QPhotoRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Photo> searchListCursor(Long lastPhotoId, int pageSize) {
        QPhoto photo = QPhoto.photo;

        BooleanBuilder builder = new BooleanBuilder();

        builder.and(photo.thumbnailStatus.eq(ThumbnailStatusEnum.READY.name()));

        if (lastPhotoId != null) {
            builder.and(photo.photoId.gt(lastPhotoId));
        }

        return queryFactory
            .selectFrom(photo)
            .where(builder)
            .orderBy(photo.photoId.asc())
            .limit(pageSize + 1)
            .fetch();
    }
}
