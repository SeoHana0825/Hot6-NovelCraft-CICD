package com.example.hot6novelcraft.domain.episode.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.domain.episode.dto.request.EpisodeCommentCreateRequest;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeCommentCreateResponse;
import com.example.hot6novelcraft.domain.episode.service.EpisodeCommentService;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/episodes")
@RequiredArgsConstructor
public class EpisodeCommentController {

    private final EpisodeCommentService episodeCommentService;

    /**
     * 댓글 작성
     */
    @PostMapping("/{episodeId}/comments")
    public ResponseEntity<BaseResponse<EpisodeCommentCreateResponse>> createComment(
            @PathVariable Long episodeId,
            @Valid @RequestBody EpisodeCommentCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        EpisodeCommentCreateResponse response =
                episodeCommentService.createComment(episodeId, request, userDetails);

        return ResponseEntity.ok(
                BaseResponse.success("OK", "댓글 작성 성공", response)
        );
    }
}