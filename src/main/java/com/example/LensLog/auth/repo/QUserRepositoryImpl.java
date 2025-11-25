package com.example.LensLog.auth.repo;

import com.example.LensLog.auth.entity.QUser;
import com.example.LensLog.auth.entity.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;


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

    @Override
    public Optional<List<User>> findNameWithEmail(String name, String email) {
        QUser user = QUser.user;

        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.isNotBlank(name)) {
            builder.and(user.name.eq(name));
        }

        if (StringUtils.isNotBlank(email)) {
            builder.and(user.email.eq(email));
        }

        List<User> userList = queryFactory
            .selectFrom(user)
            .where(builder)
            .fetch();

        return Optional.ofNullable(userList);
    }

    @Override
    public boolean existsUsernameWithEmail(String username, String email) {
        QUser user = QUser.user;

        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.isNotBlank(username)) {
            builder.and(user.username.eq(username));
        }

        if (StringUtils.isNotBlank(email)) {
            builder.and(user.email.eq(email));
        }

        User targetUser = queryFactory
            .selectFrom(user)
            .where(builder)
            .fetchOne();

        return targetUser != null ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public Optional<User> findOnlyOneUser(String provider, String username, String email) {
        QUser user = QUser.user;

        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.isNotBlank(provider)) {
            builder.and(user.provider.eq(provider));
        }

        if (StringUtils.isNotBlank(username)) {
            builder.and(user.username.eq(username));
        }

        if (StringUtils.isNotBlank(email)) {
            builder.and(user.email.eq(email));
        }

        User targetUser = queryFactory
            .selectFrom(user)
            .where(builder)
            .fetchOne();

        return Optional.ofNullable(targetUser);
    }

}
