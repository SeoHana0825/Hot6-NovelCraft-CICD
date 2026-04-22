package com.example.hot6novelcraft.domain.mentoring.repository;

import com.example.hot6novelcraft.domain.mentor.entity.QMentor;
import com.example.hot6novelcraft.domain.mentor.entity.enums.MentorStatus;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentorWithNickname;
import com.example.hot6novelcraft.domain.user.entity.QUser;
import com.example.hot6novelcraft.domain.user.entity.enums.CareerLevel;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class CustomMentorshipRepositoryImpl implements CustomMentorshipRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<MentorWithNickname> findMentorList(String genre, CareerLevel careerLevel, Pageable pageable) {
        QMentor mentor = QMentor.mentor;
        QUser user = QUser.user;

        BooleanBuilder where = new BooleanBuilder();

        // APPROVED 상태만 조회
        where.and(mentor.status.eq(MentorStatus.APPROVED));

        // 장르 필터
        if (genre != null && !genre.isBlank()) {
            where.and(mentor.mainGenres.contains("\"" + genre + "\""));
        }

        // 등급 필터
        if (careerLevel != null) {
            where.and(mentor.careerLevel.eq(careerLevel));
        }

        // Mentor + User JOIN으로 한번에 조회
        List<MentorWithNickname> content = queryFactory
                .select(Projections.constructor(
                        MentorWithNickname.class,
                        mentor.id,
                        user.nickname,
                        mentor.careerLevel,
                        mentor.mainGenres,
                        mentor.specialFields,
                        mentor.mentoringStyle,
                        mentor.awardsCareer,
                        mentor.maxMentees
                ))
                .from(mentor)
                .join(user).on(user.id.eq(mentor.userId))
                .where(where)
                .orderBy(mentor.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 총 개수
        Long total = queryFactory
                .select(mentor.count())
                .from(mentor)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}