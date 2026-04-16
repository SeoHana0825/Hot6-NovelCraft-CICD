package com.example.hot6novelcraft.domain.novel.repository;

import com.example.hot6novelcraft.domain.novel.dto.response.NovelListResponse;
import com.example.hot6novelcraft.domain.novel.entity.QNovel;
import com.example.hot6novelcraft.domain.novel.entity.enums.NovelStatus;
import com.example.hot6novelcraft.domain.user.entity.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomNovelRepositoryImpl implements CustomNovelRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<NovelListResponse> findNovelListV2(String genre, NovelStatus status, Pageable pageable) {

        QNovel novel = QNovel.novel;
        QUser user = QUser.user;

        List<NovelListResponse> content = queryFactory
                .select(Projections.constructor(NovelListResponse.class,
                        novel.id,
                        novel.title,
                        novel.genre,
                        novel.tags,
                        novel.status,
                        novel.coverImageUrl,
                        novel.viewCount,
                        novel.bookmarkCount,
                        user.nickname
                ))
                .from(novel)
                .join(user).on(novel.authorId.eq(user.id))
                .where(
                        novel.isDeleted.eq(false),
                        novel.status.ne(NovelStatus.PENDING), // PENDING 제외
                        genreEq(genre),
                        statusEq(status)
                )
                .orderBy(novel.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트
        Long total = queryFactory
                .select(novel.count())
                .from(novel)
                .join(user).on(novel.authorId.eq(user.id))
                .where(
                        novel.isDeleted.eq(false),
                        novel.status.ne(NovelStatus.PENDING),
                        genreEq(genre),
                        statusEq(status)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    // 장르 필터링 (null이면 조건X)
    private BooleanExpression genreEq(String genre) {
        return genre != null ? QNovel.novel.genre.eq(genre) : null;
    }

    // 상태 필터링 (null이면 조건X)
    private BooleanExpression statusEq(NovelStatus status) {
        return status != null ? QNovel.novel.status.eq(status) : null;
    }
}