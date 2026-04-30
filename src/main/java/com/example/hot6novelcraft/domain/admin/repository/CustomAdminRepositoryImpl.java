package com.example.hot6novelcraft.domain.admin.repository;

import com.example.hot6novelcraft.domain.mentor.entity.QMentor;
import com.example.hot6novelcraft.domain.novel.entity.QNovel;
import com.example.hot6novelcraft.domain.novel.entity.enums.NovelStatus;
import com.example.hot6novelcraft.domain.user.entity.QUser;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class CustomAdminRepositoryImpl implements CustomAdminRepository {

    private final JPAQueryFactory queryFactory;

    private final QUser user = QUser.user;
    private final QNovel novel = QNovel.novel;
    private final QMentor mentor = QMentor.mentor;

    /** ==== 회원 통계 ====
     1. 전체 회원 수 (탈퇴 회원 제외, GET /api/admin/dashboard)
     2. createdAt 기준 오늘 신규 가입 회원 수 (00시부터 현재까지)
     3. 현재 활성 회원 수 (독자+작가, 탈퇴 회원 제외) -> 접속자 회원 수 (어떤 기준으로 집계할지... 토큰 살아있는건지 등)
     ==== ↓ 필터링 조회 ↓ ====
     4. 독자 회원 수 (GET /api/admin/dashboard?role=READER)
     5. 작가 회원 수 (GET /api/admin/dashboard?role=AUTHOR)
     **/

    public Long countTotalUsers() {
        Long result = queryFactory
                .select(user.count())
                .from(user)
                .where(
                        user.isDeleted.eq(false)
                        , user.role.notIn(
                                UserRole.SUPER_ADMIN
                                , UserRole.ADMIN
                                , UserRole.PENDING_ADMIN
                                , UserRole.REJECTED_ADMIN
                        )
                )
                .fetchOne();
        return result != null ? result : 0L;
    }

    public Long countNewUsersToday() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        Long result = queryFactory
                .select(user.count())
                .from(user)
                .where(
                        user.isDeleted.eq(false)
                        , user.createdAt.goe(startOfDay)
                        , user.role.notIn(
                                        UserRole.SUPER_ADMIN
                                        , UserRole.ADMIN
                                        , UserRole.PENDING_ADMIN
                                        , UserRole.REJECTED_ADMIN
                        )
                )
                .fetchOne();
        return result != null ? result : 0L;
    }

    // null 일 경우 전체 조회, role이 있으면 해당 role만 필터링
    public Long countUsersByRole(UserRole role) {
        Long result = queryFactory
                .select(user.count())
                .from(user)
                .where(
                        user.isDeleted.eq(false)
                        , role != null
                            ? user.role.eq(role)
                                : user.role.in(UserRole.READER, UserRole.AUTHOR)
                )
                .fetchOne();
        return result != null ? result : 0L;
    }

    /** ==== 소설 통계 ====
     1. 전체 소설 수 (휴재, 보류, 삭제 포함)
     2. createdAt 기준 오늘 등록된 소설 수
     ==== ↓ 필터링 조회 ↓ ====
     3. 연재 중인 소설 수
     4. 보류 중인 소설 수
     5. 휴재 중인 소설 수
     6. 완결된 소설 수
     7. 삭제된 소설 수
     **/
    public Long countTotalNovels(String novelStatus) {
        Long result = queryFactory
                .select(novel.count())
                .from(novel)
                .where(checkNovelStatus(novelStatus))
                .fetchOne();
        return result != null ? result : 0L;
    }

    public Long countNewNovelsToday() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        Long result = queryFactory
                .select(novel.count())
                .from(novel)
                .where(novel.isDeleted.eq(false)
                        , novel.createdAt.goe(startOfDay))
                .fetchOne();
        return result != null ? result : 0L;
    }

    public Long countNovelsByFilter(NovelStatus novelStatus, Boolean isSoftDel) {
        BooleanBuilder builder = new BooleanBuilder();

        // 삭제 소설 조회 요청인 경우
        if(Boolean.TRUE.equals(isSoftDel)) {
            builder.and(novel.isDeleted.eq(true));

        } else {
            // 삭제 안 된 소설
            builder.and(novel.isDeleted.eq(false));

            // novelStatus 있으면 해당 상태만 필터
            if(novelStatus != null) {
                builder.and(novel.status.eq(novelStatus));
            }
        }

        Long result = queryFactory
                .select(novel.count())
                .from(novel)
                .where(builder)
                .fetchOne();
        return result != null ? result : 0L;
    }

    /** ==== 멘토 통계 (알려주는 사람 - 선생님) ====
     1. 전체 멘토 수 (승인/승인 안된 전체로)
     2. createdAt 기준 오늘 신청한 신규 멘토 수
     **/
    public Long countTotalMentors() {
        Long result = queryFactory
                .select(mentor.count())
                .from(mentor)
                .fetchOne();
        return result != null ? result : 0L;
    }

    public Long countNewMentorsToday() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        Long result = queryFactory
                .select(mentor.count())
                .from(mentor)
                .where(mentor.createdAt.goe(startOfDay))
                .fetchOne();
        return result != null ? result : 0L;
    }

    private BooleanExpression checkNovelStatus(String novelStatus) {
        if("ALL".equalsIgnoreCase(novelStatus)) {
            // 삭제, 보류 상관없이 전부 다 가져옴
            return null;
        }
        // ALL이 아니면 정상 소설만
        return novel.isDeleted.eq(false);
    }
}
