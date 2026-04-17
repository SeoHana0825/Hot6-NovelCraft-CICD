package com.example.hot6novelcraft.domain.mentor.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.domain.mentor.dto.request.MentorRegisterRequest;
import com.example.hot6novelcraft.domain.mentor.dto.request.MentorUpdateRequest;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorProfileResponse;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorRegisterResponse;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorUpdateResponse;
import com.example.hot6novelcraft.domain.mentor.entity.enums.MentorStatus;
import com.example.hot6novelcraft.domain.mentor.service.MentorService;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import com.example.hot6novelcraft.domain.user.entity.enums.CareerLevel;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MentorControllerTest {

    @InjectMocks
    private MentorController mentorController;

    @Mock
    private MentorService mentorService;

    private UserDetailsImpl userDetails;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("test@test.com")
                .password("password")
                .nickname("테스트유저")
                .role(UserRole.AUTHOR)
                .build();

        // Reflection으로 id 설정 (Builder에 id가 없으므로)
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, USER_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        userDetails = new UserDetailsImpl(user);
    }

    // ===================== register 테스트 =====================

    @Nested
    @DisplayName("POST /api/mentors - 멘토 등록 신청")
    class RegisterTest {

        @Test
        @DisplayName("정상 등록 시 201 반환")
        void register_success_returns_201() {
            // given
            MentorRegisterRequest request = new MentorRegisterRequest(
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

            MentorRegisterResponse mockResponse = new MentorRegisterResponse(
                    1L, MentorStatus.PENDING, LocalDateTime.now()
            );

            given(mentorService.register(eq(USER_ID), any(), any())).willReturn(mockResponse);

            // when
            ResponseEntity<BaseResponse<MentorRegisterResponse>> response =
                    mentorController.register(userDetails, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isTrue();
            assertThat(response.getBody().status()).isEqualTo("201");
            assertThat(response.getBody().data().status()).isEqualTo(MentorStatus.PENDING);
            verify(mentorService, times(1)).register(eq(USER_ID), any(), any());
        }

        @Test
        @DisplayName("자동 승인된 경우 APPROVED 상태 반환")
        void register_approved_returns_approved_status() {
            // given
            MentorRegisterRequest request = new MentorRegisterRequest(
                    "판타지 장르를 10년째 쓰고 있습니다",
                    List.of("판타지"),
                    List.of("문장력", "플롯"),
                    CareerLevel.INTRODUCTION,
                    null, null, 3, true, null
            );

            MentorRegisterResponse mockResponse = new MentorRegisterResponse(
                    1L, MentorStatus.APPROVED, LocalDateTime.now()
            );

            given(mentorService.register(eq(USER_ID), any(), any())).willReturn(mockResponse);

            // when
            ResponseEntity<BaseResponse<MentorRegisterResponse>> response =
                    mentorController.register(userDetails, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().data().status()).isEqualTo(MentorStatus.APPROVED);
        }
    }

    // ===================== update 테스트 =====================

    @Nested
    @DisplayName("PUT /api/mentors/me - 멘토 정보 수정")
    class UpdateTest {

        @Test
        @DisplayName("정상 수정 시 200 반환")
        void update_success_returns_200() {
            // given
            MentorUpdateRequest request = new MentorUpdateRequest(
                    "수정된 소개글입니다. 판타지 장르 10년차입니다",
                    List.of("판타지"),
                    List.of("문장력"),
                    "2024년 공모전 대상 수상",  // careerHistory
                    List.of("방향 제시형"),      // mentoringStyles
                    5,
                    false,
                    "성실한 분 환영합니다"
            );

            MentorUpdateResponse mockResponse = new MentorUpdateResponse(1L, LocalDateTime.now());

            given(mentorService.update(eq(USER_ID), any())).willReturn(mockResponse);

            // when
            ResponseEntity<BaseResponse<MentorUpdateResponse>> response =
                    mentorController.update(userDetails, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isTrue();
            assertThat(response.getBody().status()).isEqualTo("200");
            assertThat(response.getBody().data().mentorId()).isEqualTo(1L);
            verify(mentorService, times(1)).update(eq(USER_ID), any());
        }

        @Test
        @DisplayName("일부 필드만 수정해도 200 반환")
        void update_partial_fields_returns_200() {
            // given
            MentorUpdateRequest partialRequest = new MentorUpdateRequest(
                    null,
                    null,
                    null,
                    "경력 추가",  // careerHistory
                    null,         // mentoringStyles
                    null,
                    null,
                    null
            );

            MentorUpdateResponse mockResponse = new MentorUpdateResponse(1L, LocalDateTime.now());
            given(mentorService.update(eq(USER_ID), any())).willReturn(mockResponse);

            // when
            ResponseEntity<BaseResponse<MentorUpdateResponse>> response =
                    mentorController.update(userDetails, partialRequest);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // ===================== getMyProfile 테스트 =====================

    @Nested
    @DisplayName("GET /api/mentors/me - 내 멘토 프로필 조회")
    class GetMyProfileTest {

        @Test
        @DisplayName("정상 조회 시 200 반환")
        void getMyProfile_success_returns_200() {
            // given
            MentorProfileResponse mockResponse = new MentorProfileResponse(
                    1L,
                    "판타지 장르를 10년째 쓰고 있습니다",
                    List.of("판타지"),
                    List.of("문장력"),
                    CareerLevel.INTRODUCTION,
                    "2022 웹소설 신인상 수상",
                    List.of("꼼꼼한 피드백형"),
                    3,
                    true,
                    "연재 의지가 강한 분을 환영합니다",
                    MentorStatus.APPROVED
            );

            given(mentorService.getMyProfile(USER_ID)).willReturn(mockResponse);

            // when
            ResponseEntity<BaseResponse<MentorProfileResponse>> response =
                    mentorController.getMyProfile(userDetails);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isTrue();
            assertThat(response.getBody().status()).isEqualTo("200");
            assertThat(response.getBody().data().mentorId()).isEqualTo(1L);
            assertThat(response.getBody().data().status()).isEqualTo(MentorStatus.APPROVED);
            verify(mentorService, times(1)).getMyProfile(USER_ID);
        }

        @Test
        @DisplayName("응답 메시지 확인")
        void getMyProfile_message_check() {
            // given
            MentorProfileResponse mockResponse = new MentorProfileResponse(
                    1L, "소개글", List.of(), List.of(),
                    CareerLevel.INTRODUCTION, null, List.of(),
                    3, true, null, MentorStatus.PENDING
            );

            given(mentorService.getMyProfile(USER_ID)).willReturn(mockResponse);

            // when
            ResponseEntity<BaseResponse<MentorProfileResponse>> response =
                    mentorController.getMyProfile(userDetails);

            // then
            assertThat(response.getBody().message()).isEqualTo("내 멘토 프로필 조회 성공");
        }
    }
}