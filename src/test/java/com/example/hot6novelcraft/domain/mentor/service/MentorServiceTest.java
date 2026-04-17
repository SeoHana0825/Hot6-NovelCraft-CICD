package com.example.hot6novelcraft.domain.mentor.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.MentorExceptionEnum;
import com.example.hot6novelcraft.domain.episode.entity.enums.EpisodeStatus;
import com.example.hot6novelcraft.domain.episode.repository.EpisodeRepository;
import com.example.hot6novelcraft.domain.mentor.dto.request.MentorRegisterRequest;
import com.example.hot6novelcraft.domain.mentor.dto.request.MentorUpdateRequest;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorProfileResponse;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorRegisterResponse;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorUpdateResponse;
import com.example.hot6novelcraft.domain.mentor.entity.Mentor;
import com.example.hot6novelcraft.domain.mentor.entity.enums.MentorStatus;
import com.example.hot6novelcraft.domain.mentor.repository.MentorRepository;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import com.example.hot6novelcraft.domain.user.entity.enums.CareerLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorServiceTest {

    @InjectMocks
    private MentorService mentorService;

    @Mock
    private MentorRepository mentorRepository;

    @Mock
    private NovelRepository novelRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private ObjectMapper objectMapper;

    private static final Long USER_ID = 1L;

    private MentorRegisterRequest registerRequest;
    private MentorUpdateRequest updateRequest;
    private Mentor mentor;

    @BeforeEach
    void setUp() {
        registerRequest = new MentorRegisterRequest(
                "판타지 장르를 10년째 쓰고 있습니다",
                List.of("판타지", "로판"),
                List.of("문장력", "플롯"),
                CareerLevel.INTRODUCTION,
                "2022 웹소설 신인상 수상",
                List.of("꼼꼼한 피드백형"),
                3,
                true,
                "연재 의지가 강한 분을 환영합니다"
        );

        updateRequest = new MentorUpdateRequest(
                "수정된 소개글입니다. 판타지 장르 10년차입니다",
                List.of("판타지"),
                List.of("문장력"),
                "2024년 공모전 대상 수상",
                List.of("방향 제시형"),
                5,
                false,
                "성실한 분 환영합니다"
        );

        mentor = Mentor.create(
                USER_ID,
                CareerLevel.INTRODUCTION,
                "[\"판타지\"]",
                "[\"문장력\"]",
                "[\"꼼꼼한 피드백형\"]",
                "판타지 장르를 10년째 쓰고 있습니다",
                "2022 웹소설 신인상 수상",
                3,
                true,
                "연재 의지가 강한 분을 환영합니다",
                null,
                MentorStatus.PENDING
        );
    }

    // ===================== register 테스트 =====================

    @Nested
    @DisplayName("멘토 등록 신청")
    class RegisterTest {

        @Test
        @DisplayName("INTRODUCTION 등급 - 조건 미달 시 PENDING으로 저장")
        void register_introduction_pending() throws Exception {
            // given
            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.PENDING)).willReturn(false);
            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.APPROVED)).willReturn(false);
            given(novelRepository.findNovelIdsByAuthorId(USER_ID)).willReturn(List.of(1L));
            given(episodeRepository.countByNovelIdInAndStatus(any(), eq(EpisodeStatus.PUBLISHED))).willReturn(10L);
            given(objectMapper.writeValueAsString(any())).willReturn("[\"판타지\"]");

            ArgumentCaptor<Mentor> captor = ArgumentCaptor.forClass(Mentor.class);
            given(mentorRepository.save(captor.capture())).willReturn(mentor);

            // when
            mentorService.register(USER_ID, registerRequest, null);

            // then
            Mentor savedMentor = captor.getValue();
            assertThat(savedMentor.getStatus()).isEqualTo(MentorStatus.PENDING);
            assertThat(savedMentor.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("INTRODUCTION 등급 - 조건 충족 시 APPROVED로 저장")
        void register_introduction_approved() throws Exception {
            // given
            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.PENDING)).willReturn(false);
            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.APPROVED)).willReturn(false);
            given(novelRepository.findNovelIdsByAuthorId(USER_ID)).willReturn(List.of(1L));
            given(episodeRepository.countByNovelIdInAndStatus(any(), eq(EpisodeStatus.PUBLISHED))).willReturn(50L);
            given(objectMapper.writeValueAsString(any())).willReturn("[\"판타지\"]");

            ArgumentCaptor<Mentor> captor = ArgumentCaptor.forClass(Mentor.class);
            given(mentorRepository.save(captor.capture())).willReturn(mentor);

            // when
            mentorService.register(USER_ID, registerRequest, null);

            // then
            Mentor savedMentor = captor.getValue();
            assertThat(savedMentor.getStatus()).isEqualTo(MentorStatus.APPROVED);
        }

        @Test
        @DisplayName("ELEMENTARY 등급 - 에피소드 50개 이상 + 좋아요 50개 이상 시 APPROVED")
        void register_elementary_approved() throws Exception {
            // given
            MentorRegisterRequest elementaryRequest = new MentorRegisterRequest(
                    "판타지 장르를 10년째 쓰고 있습니다",
                    List.of("판타지"),
                    List.of("문장력", "플롯"),
                    CareerLevel.ELEMENTARY,
                    null, null, 3, true, null
            );

            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.PENDING)).willReturn(false);
            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.APPROVED)).willReturn(false);
            given(novelRepository.findNovelIdsByAuthorId(USER_ID)).willReturn(List.of(1L));
            given(episodeRepository.countByNovelIdInAndStatus(any(), eq(EpisodeStatus.PUBLISHED))).willReturn(50L);
            given(episodeRepository.sumLikeCountByNovelIdIn(any())).willReturn(50L);
            given(objectMapper.writeValueAsString(any())).willReturn("[\"판타지\"]");

            ArgumentCaptor<Mentor> captor = ArgumentCaptor.forClass(Mentor.class);
            given(mentorRepository.save(captor.capture())).willReturn(mentor);

            // when
            mentorService.register(USER_ID, elementaryRequest, null);

            // then
            assertThat(captor.getValue().getStatus()).isEqualTo(MentorStatus.APPROVED);
        }

        @Test
        @DisplayName("INTERMEDIATE 등급 - 에피소드 100개 이상 + 좋아요 100개 이상 시 APPROVED")
        void register_intermediate_approved() throws Exception {
            // given
            MentorRegisterRequest intermediateRequest = new MentorRegisterRequest(
                    "판타지 장르를 10년째 쓰고 있습니다",
                    List.of("판타지"),
                    List.of("문장력", "플롯"),
                    CareerLevel.INTERMEDIATE,
                    null, null, 3, true, null
            );

            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.PENDING)).willReturn(false);
            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.APPROVED)).willReturn(false);
            given(novelRepository.findNovelIdsByAuthorId(USER_ID)).willReturn(List.of(1L));
            given(episodeRepository.countByNovelIdInAndStatus(any(), eq(EpisodeStatus.PUBLISHED))).willReturn(100L);
            given(episodeRepository.sumLikeCountByNovelIdIn(any())).willReturn(100L);
            given(objectMapper.writeValueAsString(any())).willReturn("[\"판타지\"]");

            ArgumentCaptor<Mentor> captor = ArgumentCaptor.forClass(Mentor.class);
            given(mentorRepository.save(captor.capture())).willReturn(mentor);

            // when
            mentorService.register(USER_ID, intermediateRequest, null);

            // then
            assertThat(captor.getValue().getStatus()).isEqualTo(MentorStatus.APPROVED);
        }

        @Test
        @DisplayName("PROFICIENT 등급 - 항상 PENDING, novelRepository 조회 안 함")
        void register_proficient_always_pending() throws Exception {
            // given
            MentorRegisterRequest proficientRequest = new MentorRegisterRequest(
                    "판타지 장르를 10년째 쓰고 있습니다",
                    List.of("판타지"),
                    List.of("문장력", "플롯"),
                    CareerLevel.PROFICIENT,
                    "수상 경력", null, 3, true, null
            );

            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.PENDING)).willReturn(false);
            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.APPROVED)).willReturn(false);
            given(objectMapper.writeValueAsString(any())).willReturn("[\"판타지\"]");

            ArgumentCaptor<Mentor> captor = ArgumentCaptor.forClass(Mentor.class);
            given(mentorRepository.save(captor.capture())).willReturn(mentor);

            // when
            mentorService.register(USER_ID, proficientRequest, null);

            // then
            assertThat(captor.getValue().getStatus()).isEqualTo(MentorStatus.PENDING);
            verify(novelRepository, never()).findNovelIdsByAuthorId(any());
        }

        @Test
        @DisplayName("소설이 없으면 PENDING으로 저장, episodeRepository 조회 안 함")
        void register_no_novels_pending() throws Exception {
            // given
            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.PENDING)).willReturn(false);
            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.APPROVED)).willReturn(false);
            given(novelRepository.findNovelIdsByAuthorId(USER_ID)).willReturn(List.of());
            given(objectMapper.writeValueAsString(any())).willReturn("[\"판타지\"]");

            ArgumentCaptor<Mentor> captor = ArgumentCaptor.forClass(Mentor.class);
            given(mentorRepository.save(captor.capture())).willReturn(mentor);

            // when
            mentorService.register(USER_ID, registerRequest, null);

            // then
            assertThat(captor.getValue().getStatus()).isEqualTo(MentorStatus.PENDING);
            verify(episodeRepository, never()).countByNovelIdInAndStatus(any(), any());
        }

        @Test
        @DisplayName("이미 PENDING 상태인 경우 예외 발생")
        void register_pending_exists_throws() {
            // given
            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.PENDING)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> mentorService.register(USER_ID, registerRequest, null))
                    .isInstanceOf(ServiceErrorException.class)
                    .hasMessage(MentorExceptionEnum.MENTOR_PENDING_EXISTS.getMessage());

            verify(mentorRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 APPROVED 상태인 경우 예외 발생")
        void register_approved_exists_throws() {
            // given
            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.PENDING)).willReturn(false);
            given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.APPROVED)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> mentorService.register(USER_ID, registerRequest, null))
                    .isInstanceOf(ServiceErrorException.class)
                    .hasMessage(MentorExceptionEnum.MENTOR_ALREADY_APPROVED.getMessage());

            verify(mentorRepository, never()).save(any());
        }
    }

    // ===================== update 테스트 =====================

    @Nested
    @DisplayName("멘토 정보 수정")
    class UpdateTest {

        @Test
        @DisplayName("정상적으로 멘토 정보 수정")
        void update_success() throws Exception {
            // given
            given(mentorRepository.findByUserId(USER_ID)).willReturn(Optional.of(mentor));
            given(objectMapper.writeValueAsString(any())).willReturn("[\"판타지\"]");

            // when
            MentorUpdateResponse response = mentorService.update(USER_ID, updateRequest);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("멘토 프로필이 없으면 예외 발생")
        void update_mentor_not_found_throws() {
            // given
            given(mentorRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> mentorService.update(USER_ID, updateRequest))
                    .isInstanceOf(ServiceErrorException.class)
                    .hasMessage(MentorExceptionEnum.MENTOR_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("careerHistory가 빈 문자열이면 예외 발생")
        void update_blank_career_history_throws() {
            // given
            MentorUpdateRequest blankCareerRequest = new MentorUpdateRequest(
                    "수정된 소개글입니다", null, null, "", null, null, null, null
            );
            given(mentorRepository.findByUserId(USER_ID)).willReturn(Optional.of(mentor));

            // when & then
            assertThatThrownBy(() -> mentorService.update(USER_ID, blankCareerRequest))
                    .isInstanceOf(ServiceErrorException.class)
                    .hasMessage(MentorExceptionEnum.MENTOR_CAREER_REQUIRED.getMessage());
        }

        @Test
        @DisplayName("careerHistory가 null이면 기존 값 유지")
        void update_null_career_history_keeps_existing() throws Exception {
            // given
            MentorUpdateRequest nullCareerRequest = new MentorUpdateRequest(
                    null, null, null, null, null, null, null, null
            );
            given(mentorRepository.findByUserId(USER_ID)).willReturn(Optional.of(mentor));

            // when
            MentorUpdateResponse response = mentorService.update(USER_ID, nullCareerRequest);

            // then
            assertThat(response).isNotNull();
        }
    }

    // ===================== getMyProfile 테스트 =====================

    @Nested
    @DisplayName("내 멘토 프로필 조회")
    class GetMyProfileTest {

        @Test
        @DisplayName("정상적으로 프로필 조회")
        void getMyProfile_success() {
            // given
            given(mentorRepository.findByUserId(USER_ID)).willReturn(Optional.of(mentor));

            // when
            MentorProfileResponse response = mentorService.getMyProfile(USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(MentorStatus.PENDING);
            assertThat(response.careerLevel()).isEqualTo(CareerLevel.INTRODUCTION);
        }

        @Test
        @DisplayName("멘토 프로필이 없으면 예외 발생")
        void getMyProfile_not_found_throws() {
            // given
            given(mentorRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> mentorService.getMyProfile(USER_ID))
                    .isInstanceOf(ServiceErrorException.class)
                    .hasMessage(MentorExceptionEnum.MENTOR_NOT_FOUND.getMessage());
        }
    }
    @Test
    @DisplayName("동시 요청으로 DataIntegrityViolationException 발생 시 MENTOR_ALREADY_APPROVED 예외로 변환")
    void register_data_integrity_violation_throws() throws Exception {
        // given
        given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.PENDING)).willReturn(false);
        given(mentorRepository.existsByUserIdAndStatus(USER_ID, MentorStatus.APPROVED)).willReturn(false);
        given(novelRepository.findNovelIdsByAuthorId(USER_ID)).willReturn(List.of());
        given(objectMapper.writeValueAsString(any())).willReturn("[\"판타지\"]");
        given(mentorRepository.save(any())).willThrow(DataIntegrityViolationException.class);

        // when & then
        assertThatThrownBy(() -> mentorService.register(USER_ID, registerRequest, null))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(MentorExceptionEnum.MENTOR_ALREADY_APPROVED.getMessage());
    }
}
