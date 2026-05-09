package com.example.hot6novelcraft.domain.reviewai.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.domain.episode.entity.Episode;
import com.example.hot6novelcraft.domain.episode.entity.enums.EpisodeStatus;
import com.example.hot6novelcraft.domain.episode.repository.EpisodeRepository;
import com.example.hot6novelcraft.domain.novel.entity.Novel;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import com.example.hot6novelcraft.domain.point.service.PointService;
import com.example.hot6novelcraft.domain.reviewai.client.AiReviewClient;
import com.example.hot6novelcraft.domain.reviewai.dto.cache.AiReviewJob;
import com.example.hot6novelcraft.domain.reviewai.dto.event.AiReviewMessage;
import com.example.hot6novelcraft.domain.reviewai.dto.response.AiReviewJobResponse;
import com.example.hot6novelcraft.domain.reviewai.dto.response.AiReviewResponse;
import com.example.hot6novelcraft.domain.reviewai.entity.enums.AiReviewJobStatus;
import com.example.hot6novelcraft.domain.reviewai.producer.AiReviewProducer;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AiReviewServiceTest {

    @Mock EpisodeRepository episodeRepository;
    @Mock NovelRepository novelRepository;
    @Mock AiReviewClient aiReviewClient;
    @Mock AiReviewProducer aiReviewProducer;
    @Mock PointService pointService;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOperations;
    @Mock ObjectMapper objectMapper;

    @InjectMocks
    AiReviewService aiReviewService;

    // ===================== Mock 헬퍼 메서드 =====================

    private UserDetailsImpl 작가() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(user.getRole()).willReturn(UserRole.AUTHOR);
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        given(userDetails.getUser()).willReturn(user);
        return userDetails;
    }

    private UserDetailsImpl 독자() {
        User user = mock(User.class);
        given(user.getId()).willReturn(2L);
        given(user.getRole()).willReturn(UserRole.READER);
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        given(userDetails.getUser()).willReturn(user);
        return userDetails;
    }

    private Novel 소설(Long authorId) {
        Novel novel = mock(Novel.class);
        given(novel.getId()).willReturn(1L);
        given(novel.getAuthorId()).willReturn(authorId);
        return novel;
    }

    private Episode 회차(EpisodeStatus status, String content) {
        Episode episode = mock(Episode.class);
        given(episode.getId()).willReturn(1L);
        given(episode.getNovelId()).willReturn(1L);
        given(episode.getTitle()).willReturn("테스트 회차");
        given(episode.getContent()).willReturn(content);
        given(episode.getStatus()).willReturn(status);
        given(episode.isDeleted()).willReturn(false);
        return episode;
    }

    private AiReviewResponse AI리뷰응답() {
        return new AiReviewResponse(
                1L,
                List.of(
                        new AiReviewResponse.AiCommentResponse("달빛독자", "와 미쳤다 😭", 4.5),
                        new AiReviewResponse.AiCommentResponse("소설덕후", "다음화 빨리요!", 4.0)
                )
        );
    }

    private AiReviewJob PROCESSING상태_Job() {
        return AiReviewJob.create("test-job-id", 1L, 1L);
    }

    private AiReviewJob COMPLETED상태_Job() {
        return PROCESSING상태_Job().completed(AI리뷰응답());
    }

    private AiReviewJob FAILED상태_Job() {
        return PROCESSING상태_Job().failed("AI 리뷰 생성에 실패했습니다.");
    }

    // Redis Mocking 헬퍼
    private void Redis_Job저장_설정() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    private void Redis_Job조회_설정(String jobJson) {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(jobJson);
    }

    // ===================== v1 - getReview() =====================

    @Test
    void v1_AI리뷰_성공() throws Exception {
        UserDetailsImpl userDetails = 작가();
        Episode episode = 회차(EpisodeStatus.DRAFT, "소설 본문 내용입니다.");
        Novel novel = 소설(1L);

        given(episodeRepository.findById(1L)).willReturn(Optional.of(episode));
        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));
        given(pointService.getBalance(1L)).willReturn(500L);
        given(aiReviewClient.generate(anyLong(), anyString(), anyString())).willReturn(AI리뷰응답());

        AiReviewResponse result = aiReviewService.getReview(1L, userDetails);

        assertNotNull(result);
        assertEquals(2, result.comments().size());
        verify(pointService).deductForAi(1L, 200L, 1L);
    }

    @Test
    void v1_작가권한없으면_실패() {
        UserDetailsImpl userDetails = 독자();

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.getReview(1L, userDetails));
    }

    @Test
    void v1_회차없으면_실패() {
        UserDetailsImpl userDetails = 작가();

        given(episodeRepository.findById(1L)).willReturn(Optional.empty());

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.getReview(1L, userDetails));
    }

    @Test
    void v1_삭제된회차면_실패() {
        UserDetailsImpl userDetails = 작가();
        Episode episode = 회차(EpisodeStatus.DRAFT, "내용");
        given(episode.isDeleted()).willReturn(true);

        given(episodeRepository.findById(1L)).willReturn(Optional.of(episode));

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.getReview(1L, userDetails));
    }

    @Test
    void v1_본인소설아니면_실패() {
        UserDetailsImpl userDetails = 작가(); // userId = 1L
        Episode episode = 회차(EpisodeStatus.DRAFT, "내용");
        Novel novel = 소설(2L); // authorId = 2L (다른 작가)

        given(episodeRepository.findById(1L)).willReturn(Optional.of(episode));
        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.getReview(1L, userDetails));
    }

    @Test
    void v1_DRAFT아니면_실패() {
        UserDetailsImpl userDetails = 작가();
        Episode episode = 회차(EpisodeStatus.PUBLISHED, "내용");
        Novel novel = 소설(1L);

        given(episodeRepository.findById(1L)).willReturn(Optional.of(episode));
        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.getReview(1L, userDetails));
    }

    @Test
    void v1_본문비어있으면_실패() {
        UserDetailsImpl userDetails = 작가();
        Episode episode = 회차(EpisodeStatus.DRAFT, "");
        Novel novel = 소설(1L);

        given(episodeRepository.findById(1L)).willReturn(Optional.of(episode));
        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.getReview(1L, userDetails));
    }

    @Test
    void v1_포인트부족하면_실패() {
        UserDetailsImpl userDetails = 작가();
        Episode episode = 회차(EpisodeStatus.DRAFT, "소설 본문 내용입니다.");
        Novel novel = 소설(1L);

        given(episodeRepository.findById(1L)).willReturn(Optional.of(episode));
        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));
        given(pointService.getBalance(1L)).willReturn(100L); // 200P 부족

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.getReview(1L, userDetails));
    }

    @Test
    void v1_OpenAI실패시_예외발생하고_포인트차감안됨() {
        UserDetailsImpl userDetails = 작가();
        Episode episode = 회차(EpisodeStatus.DRAFT, "소설 본문 내용입니다.");
        Novel novel = 소설(1L);

        given(episodeRepository.findById(1L)).willReturn(Optional.of(episode));
        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));
        given(pointService.getBalance(1L)).willReturn(500L);
        given(aiReviewClient.generate(anyLong(), anyString(), anyString()))
                .willThrow(new RuntimeException("OpenAI 호출 실패"));

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.getReview(1L, userDetails));

        // 포인트 차감 안됨
        verify(pointService, never()).deductForAi(anyLong(), anyLong(), anyLong());
    }

    // ===================== v2 - requestReviewAsync() =====================

    @Test
    void v2_비동기요청_성공_jobId반환() throws Exception {
        UserDetailsImpl userDetails = 작가();
        Episode episode = 회차(EpisodeStatus.DRAFT, "소설 본문 내용입니다.");
        Novel novel = 소설(1L);

        given(episodeRepository.findById(1L)).willReturn(Optional.of(episode));
        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));
        given(pointService.getBalance(1L)).willReturn(500L);
        Redis_Job저장_설정();
        given(objectMapper.writeValueAsString(any())).willReturn("{}");
        given(aiReviewProducer.send(any())).willReturn(CompletableFuture.completedFuture(null));

        AiReviewJobResponse result = aiReviewService.requestReviewAsync(1L, userDetails);

        assertNotNull(result);
        assertNotNull(result.jobId());
        assertEquals(AiReviewJobStatus.PROCESSING, result.status());
    }

    @Test
    void v2_작가권한없으면_실패() {
        UserDetailsImpl userDetails = 독자();

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.requestReviewAsync(1L, userDetails));
    }

    @Test
    void v2_본인소설아니면_실패() {
        UserDetailsImpl userDetails = 작가(); // userId = 1L
        Episode episode = 회차(EpisodeStatus.DRAFT, "내용");
        Novel novel = 소설(2L); // authorId = 2L

        given(episodeRepository.findById(1L)).willReturn(Optional.of(episode));
        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.requestReviewAsync(1L, userDetails));
    }

    @Test
    void v2_DRAFT아니면_실패() {
        UserDetailsImpl userDetails = 작가();
        Episode episode = 회차(EpisodeStatus.PUBLISHED, "내용");
        Novel novel = 소설(1L);

        given(episodeRepository.findById(1L)).willReturn(Optional.of(episode));
        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.requestReviewAsync(1L, userDetails));
    }

    @Test
    void v2_포인트부족하면_Kafka발행안함() {
        UserDetailsImpl userDetails = 작가();
        Episode episode = 회차(EpisodeStatus.DRAFT, "소설 본문 내용입니다.");
        Novel novel = 소설(1L);

        given(episodeRepository.findById(1L)).willReturn(Optional.of(episode));
        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));
        given(pointService.getBalance(1L)).willReturn(100L); // 200P 부족

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.requestReviewAsync(1L, userDetails));

        // Kafka 발행 안됨
        verify(aiReviewProducer, never()).send(any());
    }

    // ===================== processReview() =====================

    @Test
    void processReview_성공_Job_COMPLETED처리() throws Exception {
        AiReviewMessage message = new AiReviewMessage(
                "test-job-id", 1L, 1L, "테스트 제목", "소설 본문 내용입니다."
        );

        // ↓ 실제 직렬화 안 하고 그냥 더미 문자열
        Redis_Job조회_설정("{}");
        given(objectMapper.readValue(anyString(), eq(AiReviewJob.class)))
                .willReturn(PROCESSING상태_Job());
        given(objectMapper.writeValueAsString(any())).willReturn("{}");
        given(aiReviewClient.generate(anyLong(), anyString(), anyString()))
                .willReturn(AI리뷰응답());

        aiReviewService.processReview(message);

        verify(pointService).deductForAi(1L, 200L, 1L);
        verify(valueOperations, atLeastOnce()).set(anyString(), anyString(), any());
    }

    @Test
    void processReview_Job없으면_스킵() throws Exception {
        AiReviewMessage message = new AiReviewMessage(
                "test-job-id", 1L, 1L, "테스트 제목", "소설 본문"
        );

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null); // Job 없음

        aiReviewService.processReview(message);

        // OpenAI 호출 안됨
        verify(aiReviewClient, never()).generate(anyLong(), anyString(), anyString());
        // 포인트 차감 안됨
        verify(pointService, never()).deductForAi(anyLong(), anyLong(), anyLong());
    }

    @Test
    void processReview_이미완료된_Job이면_스킵() throws Exception {
        AiReviewMessage message = new AiReviewMessage(
                "test-job-id", 1L, 1L, "테스트 제목", "소설 본문"
        );

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn("{}");
        given(objectMapper.readValue(anyString(), eq(AiReviewJob.class)))
                .willReturn(COMPLETED상태_Job()); // 이미 COMPLETED

        aiReviewService.processReview(message);

        // OpenAI 호출 안됨 (중복 처리 방지)
        verify(aiReviewClient, never()).generate(anyLong(), anyString(), anyString());
        // 포인트 차감 안됨
        verify(pointService, never()).deductForAi(anyLong(), anyLong(), anyLong());
    }

    @Test
    void processReview_이미실패한_Job이면_스킵() throws Exception {
        AiReviewMessage message = new AiReviewMessage(
                "test-job-id", 1L, 1L, "테스트 제목", "소설 본문"
        );

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn("{}");
        given(objectMapper.readValue(anyString(), eq(AiReviewJob.class)))
                .willReturn(FAILED상태_Job()); // 이미 FAILED

        aiReviewService.processReview(message);

        // OpenAI 호출 안됨
        verify(aiReviewClient, never()).generate(anyLong(), anyString(), anyString());
    }

    @Test
    void processReview_OpenAI실패시_Job_FAILED처리_포인트차감안됨() throws Exception {
        AiReviewMessage message = new AiReviewMessage(
                "test-job-id", 1L, 1L, "테스트 제목", "소설 본문 내용입니다."
        );

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn("{}");
        given(objectMapper.readValue(anyString(), eq(AiReviewJob.class)))
                .willReturn(PROCESSING상태_Job());
        given(objectMapper.writeValueAsString(any())).willReturn("{}");
        given(aiReviewClient.generate(anyLong(), anyString(), anyString()))
                .willThrow(new RuntimeException("OpenAI 호출 실패"));

        aiReviewService.processReview(message);

        // 포인트 차감 안됨
        verify(pointService, never()).deductForAi(anyLong(), anyLong(), anyLong());
    }

    @Test
    void processReview_포인트차감실패시_Job_FAILED처리() throws Exception {
        AiReviewMessage message = new AiReviewMessage(
                "test-job-id", 1L, 1L, "테스트 제목", "소설 본문 내용입니다."
        );

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn("{}");
        given(objectMapper.readValue(anyString(), eq(AiReviewJob.class)))
                .willReturn(PROCESSING상태_Job());
        given(objectMapper.writeValueAsString(any())).willReturn("{}");
        given(aiReviewClient.generate(anyLong(), anyString(), anyString()))
                .willReturn(AI리뷰응답());
        doThrow(new RuntimeException("포인트 부족"))
                .when(pointService).deductForAi(anyLong(), anyLong(), anyLong());

        aiReviewService.processReview(message);

        // Job이 FAILED 상태로 저장됨 (save 호출됨)
        verify(valueOperations, atLeastOnce()).set(anyString(), anyString(), any());
    }

    // ===================== getJobStatus() =====================

    @Test
    void getJobStatus_성공() throws Exception {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn("{}");
        given(objectMapper.readValue(anyString(), eq(AiReviewJob.class)))
                .willReturn(COMPLETED상태_Job());

        AiReviewJob result = aiReviewService.getJobStatus("test-job-id");

        assertNotNull(result);
        assertEquals(AiReviewJobStatus.COMPLETED, result.status());
    }

    @Test
    void getJobStatus_없는jobId면_실패() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);

        assertThrows(ServiceErrorException.class,
                () -> aiReviewService.getJobStatus("없는-job-id"));
    }

    @Test
    void getJobStatus_PROCESSING상태_조회() throws Exception {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn("{}");
        given(objectMapper.readValue(anyString(), eq(AiReviewJob.class)))
                .willReturn(PROCESSING상태_Job());

        AiReviewJob result = aiReviewService.getJobStatus("test-job-id");

        assertEquals(AiReviewJobStatus.PROCESSING, result.status());
        assertNull(result.result());
    }

    @Test
    void getJobStatus_FAILED상태_조회() throws Exception {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn("{}");
        given(objectMapper.readValue(anyString(), eq(AiReviewJob.class)))
                .willReturn(FAILED상태_Job());

        AiReviewJob result = aiReviewService.getJobStatus("test-job-id");

        assertEquals(AiReviewJobStatus.FAILED, result.status());
        assertNotNull(result.errorMessage());
    }
}