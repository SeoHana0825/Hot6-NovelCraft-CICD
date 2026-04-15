package com.example.hot6novelcraft.domain.episode.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.domain.episode.dto.request.EpisodeCreateRequest;
import com.example.hot6novelcraft.domain.episode.dto.request.EpisodeUpdateRequest;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeCreateResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeDeleteResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodePublishResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeUpdateResponse;
import com.example.hot6novelcraft.domain.episode.service.EpisodeService;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api")
public class EpisodeController {

    private final EpisodeService episodeService;

    /**
     * 회차 생성
     * 정은식
     */
    @PostMapping("/novels/{novelId}/episodes")
    public ResponseEntity<BaseResponse<EpisodeCreateResponse>> createEpisode(
            @PathVariable Long novelId,
            @Valid @RequestBody EpisodeCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        EpisodeCreateResponse response = episodeService.createEpisode(novelId, request, userDetails);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("201", "회차 생성 성공", response));
    }

    /**
     * 회차 수정
     * 정은식
     */
    @PatchMapping("/episodes/{episodeId}")
    public ResponseEntity<BaseResponse<EpisodeUpdateResponse>> updateEpisode(
            @PathVariable Long episodeId,
            @Valid @RequestBody EpisodeUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        EpisodeUpdateResponse response = episodeService.updateEpisode(episodeId, request, userDetails);

        return ResponseEntity.ok(
                BaseResponse.success("200", "회차 수정 성공", response)
        );
    }

    /**
     * 회차 삭제
     * 정은식
     */
    @DeleteMapping("/episodes/{episodeId}")
    public ResponseEntity<BaseResponse<EpisodeDeleteResponse>> deleteEpisode(
            @PathVariable Long episodeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        EpisodeDeleteResponse response = episodeService.deleteEpisode(episodeId, userDetails);

        return ResponseEntity.ok(
                BaseResponse.success("200", "회차 삭제 성공", response)
        );
    }

    /**
     * 회차 발행
     * 정은식
     */
    @PostMapping("/episodes/{episodeId}/publish")
    public ResponseEntity<BaseResponse<EpisodePublishResponse>> publishEpisode(
            @PathVariable Long episodeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        EpisodePublishResponse response = episodeService.publishEpisode(episodeId, userDetails);

        return ResponseEntity.ok(
                BaseResponse.success("200", "회차 발행 성공", response)
        );
    }
}