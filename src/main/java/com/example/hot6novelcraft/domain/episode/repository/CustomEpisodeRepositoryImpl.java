package com.example.hot6novelcraft.domain.episode.repository;

import com.example.hot6novelcraft.domain.episode.dto.response.AuthorEpisodeListResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeListResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeMetaDto;
import com.example.hot6novelcraft.domain.episode.entity.Episode;
import com.example.hot6novelcraft.domain.episode.entity.QEpisode;
import com.example.hot6novelcraft.domain.episode.entity.enums.EpisodeStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.hot6novelcraft.domain.episode.entity.QEpisode.episode;

@Repository
@RequiredArgsConstructor
public class CustomEpisodeRepositoryImpl implements CustomEpisodeRepository {

    private final JPAQueryFactory queryFactory;

    // 회차 목록 조회
    @Override
    public Page<EpisodeListResponse> findEpisodeListByNovelId(Long novelId, Pageable pageable) {

        QEpisode episode = QEpisode.episode;

        List<EpisodeListResponse> content = queryFactory
                .select(Projections.constructor(EpisodeListResponse.class,
                        episode.id,
                        episode.episodeNumber,
                        episode.title,
                        episode.isFree,
                        episode.pointPrice,
                        episode.likeCount,
                        episode.publishedAt
                ))
                .from(episode)
                .where(
                        episode.novelId.eq(novelId),
                        episode.status.eq(EpisodeStatus.PUBLISHED), // 발행한것만
                        episode.isDeleted.eq(false) // 삭제 된건지 확인
                )
                .orderBy(episode.episodeNumber.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(episode.count())
                .from(episode)
                .where(
                        episode.novelId.eq(novelId),
                        episode.status.eq(EpisodeStatus.PUBLISHED),
                        episode.isDeleted.eq(false)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public List<Episode> findBulkEpisodes(Long novelId, int startNumber, int endNumber) {

        return queryFactory
                .selectFrom(episode)
                .where(
                        episode.novelId.eq(novelId),
                        episode.episodeNumber.between(startNumber, endNumber),
                        episode.status.eq(EpisodeStatus.PUBLISHED),  // 발행된 회차만
                        episode.isDeleted.isFalse()                   // 삭제 안 된 것만
                )
                .orderBy(episode.episodeNumber.asc())
                .fetch();
    }

    // 본문 텍스트 제외한 조회
    @Override
    public EpisodeMetaDto findMetaById(Long episodeId) {
        return queryFactory
                .select(Projections.constructor(EpisodeMetaDto.class,
                        episode.id,
                        episode.novelId,
                        episode.episodeNumber,
                        episode.isFree,
                        episode.pointPrice,
                        episode.status,
                        episode.isDeleted
                ))
                .from(episode)
                .where(episode.id.eq(episodeId))
                .fetchOne();
    }

    // 작가용 회차 목록 조회 (본인 소설의 회차, DRAFT 포함)
    @Override
    public Page<AuthorEpisodeListResponse> findAuthorEpisodeList(Long novelId, Pageable pageable) {

        List<AuthorEpisodeListResponse> content = queryFactory
                .select(Projections.constructor(AuthorEpisodeListResponse.class,
                        episode.id,
                        episode.episodeNumber,
                        episode.title,
                        episode.status,
                        episode.isFree,
                        episode.pointPrice,
                        episode.publishedAt,
                        episode.updatedAt
                ))
                .from(episode)
                .where(
                        episode.novelId.eq(novelId),
                        episode.isDeleted.eq(false)
                )
                .orderBy(episode.episodeNumber.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(episode.count())
                .from(episode)
                .where(
                        episode.novelId.eq(novelId),
                        episode.isDeleted.eq(false)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}