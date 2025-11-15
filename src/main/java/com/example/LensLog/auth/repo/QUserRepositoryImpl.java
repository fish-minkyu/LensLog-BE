package com.example.LensLog.auth.repo;

import com.example.LensLog.auth.entity.QUser;
import com.example.LensLog.auth.entity.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
public class QUserRepositoryImpl implements QUserRepository{
    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsUserWithProvider(String provider, String username) {
        QUser user = QUser.user;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(user.provider.eq(provider));
        builder.and(user.username.eq(username));

        User targetUser = queryFactory
            .selectFrom(user)
            .where(builder)
            .fetchOne();

        return targetUser != null ? Boolean.TRUE : Boolean.FALSE;
    }
}
