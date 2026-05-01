package com.example.hot6novelcraft.domain.admin.service;

import com.example.hot6novelcraft.domain.admin.dto.response.AdminDashboardResponse;
import com.example.hot6novelcraft.domain.admin.repository.CustomAdminRepositoryImpl;
import com.example.hot6novelcraft.domain.novel.entity.enums.NovelStatus;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDashboardService 단위 테스트")
class AdminDashboardServiceTest {

    // 실제 DB 연결 없이 가짜(Mock) 객체로 대체
    @Mock
    private CustomAdminRepositoryImpl adminRepository;

    // Mock을 주입받은 실제 Service 객체 생성
    @InjectMocks
    private AdminDashboardService adminDashboardService;

    // =============================================
    // 성공 케이스
    // =============================================
    @Nested
    @DisplayName("성공 케이스")
    class SuccessCase {

        @Test
        @DisplayName("필터 없이 전체 통계 조회 성공")
        void getDashboardStats_전체조회_성공() {
            // given - Mock 반환값 설정
            given(adminRepository.countTotalUsers()).willReturn(100L);
            given(adminRepository.countNewUsersToday()).willReturn(5L);
            given(adminRepository.countUsersByRole(null)).willReturn(90L);  // 전체 일반 회원
            given(adminRepository.countTotalNovels(any())).willReturn(200L);
            given(adminRepository.countNewNovelsToday()).willReturn(3L);
            given(adminRepository.countNovelsByFilter(null, null)).willReturn(200L); // 전체 소설
            given(adminRepository.countTotalMentors()).willReturn(30L);
            given(adminRepository.countNewMentorsToday()).willReturn(1L);

            // when - 필터 없이 전체 조회
            AdminDashboardResponse result =
                    adminDashboardService.getDashboardStatus(null, null, null);

            // then - 응답값 검증
            assertThat(result).isNotNull();

            // 회원 통계 검증
            assertThat(result.userStatus().totalUsers()).isEqualTo(100L);
            assertThat(result.userStatus().newUsersToday()).isEqualTo(5L);
            assertThat(result.userStatus().filterUserRole()).isEqualTo(90L);

            // 소설 통계 검증
            assertThat(result.novelStatus().totalNovels()).isEqualTo(200L);
            assertThat(result.novelStatus().newNovelsToday()).isEqualTo(3L);
            assertThat(result.novelStatus().novelsByFilter()).isEqualTo(200L);

            // 멘토 통계 검증
            assertThat(result.mentorStatus().totalMentor()).isEqualTo(30L);
            assertThat(result.mentorStatus().newMentorsToday()).isEqualTo(1L);

            // Repository 메서드 호출 여부 검증
            verify(adminRepository, times(1)).countTotalUsers();
            verify(adminRepository, times(1)).countTotalNovels(any());
            verify(adminRepository, times(1)).countTotalMentors();
        }

        @Test
        @DisplayName("role=READER 필터 조회 성공")
        void getDashboardStats_독자필터_성공() {
            // given
            given(adminRepository.countTotalUsers()).willReturn(100L);
            given(adminRepository.countNewUsersToday()).willReturn(5L);
            given(adminRepository.countUsersByRole(UserRole.READER)).willReturn(60L); // 독자만
            given(adminRepository.countTotalNovels(any())).willReturn(200L);
            given(adminRepository.countNewNovelsToday()).willReturn(3L);
            given(adminRepository.countNovelsByFilter(null, null)).willReturn(200L);
            given(adminRepository.countTotalMentors()).willReturn(30L);
            given(adminRepository.countNewMentorsToday()).willReturn(1L);

            // when
            AdminDashboardResponse result =
                    adminDashboardService.getDashboardStatus(UserRole.READER, null, null);

            // then
            assertThat(result.userStatus().filterUserRole()).isEqualTo(60L); // 독자 수만
            verify(adminRepository, times(1)).countUsersByRole(UserRole.READER);
        }

        @Test
        @DisplayName("novelStatus=ONGOING 필터 조회 성공")
        void getDashboardStats_연재중필터_성공() {
            // given
            given(adminRepository.countTotalUsers()).willReturn(100L);
            given(adminRepository.countNewUsersToday()).willReturn(5L);
            given(adminRepository.countUsersByRole(null)).willReturn(90L);
            given(adminRepository.countTotalNovels(any())).willReturn(200L);
            given(adminRepository.countNewNovelsToday()).willReturn(3L);
            given(adminRepository.countNovelsByFilter(NovelStatus.ONGOING, null)).willReturn(100L); // 연재 중만
            given(adminRepository.countTotalMentors()).willReturn(30L);
            given(adminRepository.countNewMentorsToday()).willReturn(1L);

            // when
            AdminDashboardResponse result =
                    adminDashboardService.getDashboardStatus(null, NovelStatus.ONGOING.name(), null);

            // then
            assertThat(result.novelStatus().novelsByFilter()).isEqualTo(100L); // 연재 중 소설 수
            verify(adminRepository, times(1)).countNovelsByFilter(NovelStatus.ONGOING, null);
        }

        @Test
        @DisplayName("isDeleted=true 삭제 소설 필터 조회 성공")
        void getDashboardStats_삭제소설필터_성공() {
            // given
            given(adminRepository.countTotalUsers()).willReturn(100L);
            given(adminRepository.countNewUsersToday()).willReturn(5L);
            given(adminRepository.countUsersByRole(null)).willReturn(90L);
            given(adminRepository.countTotalNovels(any())).willReturn(200L);
            given(adminRepository.countNewNovelsToday()).willReturn(3L);
            given(adminRepository.countNovelsByFilter(null, true)).willReturn(10L); // 삭제된 소설만
            given(adminRepository.countTotalMentors()).willReturn(30L);
            given(adminRepository.countNewMentorsToday()).willReturn(1L);

            // when
            AdminDashboardResponse result =
                    adminDashboardService.getDashboardStatus(null, null, true);

            // then
            assertThat(result.novelStatus().novelsByFilter()).isEqualTo(10L);
            verify(adminRepository, times(1)).countNovelsByFilter(null, true);
        }

        @Test
        @DisplayName("데이터 없을 때 0으로 반환 성공")
        void getDashboardStats_데이터없음_0반환() {
            // given - 모든 값 0으로 설정
            given(adminRepository.countTotalUsers()).willReturn(0L);
            given(adminRepository.countNewUsersToday()).willReturn(0L);
            given(adminRepository.countUsersByRole(null)).willReturn(0L);
            given(adminRepository.countTotalNovels(any())).willReturn(0L);
            given(adminRepository.countNewNovelsToday()).willReturn(0L);
            given(adminRepository.countNovelsByFilter(null, null)).willReturn(0L);
            given(adminRepository.countTotalMentors()).willReturn(0L);
            given(adminRepository.countNewMentorsToday()).willReturn(0L);

            // when
            AdminDashboardResponse result =
                    adminDashboardService.getDashboardStatus(null, null, null);

            // then - null이 아닌 0으로 반환되는지 검증
            assertThat(result.userStatus().totalUsers()).isZero();
            assertThat(result.novelStatus().totalNovels()).isZero();
            assertThat(result.mentorStatus().totalMentor()).isZero();
        }
    }

    // =============================================
    // 실패 케이스
    // =============================================
    @Nested
    @DisplayName("실패 케이스")
    class FailCase {

        @Test
        @DisplayName("Repository 예외 발생 시 전파")
        void getDashboardStats_Repository예외_전파() {
            // given - Repository에서 예외 던지도록 설정
            given(adminRepository.countTotalUsers())
                    .willThrow(new RuntimeException("DB 연결 실패"));

            // when & then - 예외가 그대로 전파되는지 검증
            org.junit.jupiter.api.Assertions.assertThrows(
                    RuntimeException.class,
                    () -> adminDashboardService.getDashboardStatus(null, null, null)
            );
        }
    }
}