package com.example.LensLog.photo.repo;

import com.example.LensLog.good.entity.Good;
import com.example.LensLog.good.entity.QGood;
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

    // Cursor 페이지네이션(카테고리별)
    @Override
    public List<Photo> searchListCursor(Long categoryId, Long lastPhotoId, int pageSize) {
        QPhoto photo = QPhoto.photo;

        BooleanBuilder builder = new BooleanBuilder();

        if (categoryId != null) {
            builder.and(photo.category.categoryId.eq(categoryId));
        }

        if (lastPhotoId != null) {
            builder.and(photo.photoId.gt(lastPhotoId));
        }

        // 썸네일이 생성된 것만 가지고 오기.
         builder.and(photo.thumbnailStatus.eq(ThumbnailStatusEnum.READY.name()));

        return queryFactory
            .selectFrom(photo)
            .where(builder)
            .orderBy(photo.photoId.asc())
            .limit(pageSize + 1)
            .fetch();
    }

    @Override
    public List<Long> getListGoodPhotoIdByUserId(Long userId) {
        QPhoto photo = QPhoto.photo;
        QGood good = QGood.good;

        return queryFactory
            .select(good.photo.photoId)
            .from(good)
            .join(good.photo, photo)
            .where(good.userId.eq(userId))
            .fetch();
    }

    @Override
    public List<Photo> getListPhotoCursorByLike(List<Long> photoGoodList) {
        QPhoto photo = QPhoto.photo;

        return queryFactory
            .selectFrom(photo)
            .where(photo.photoId.in(photoGoodList))
            .fetch();
    }


}
