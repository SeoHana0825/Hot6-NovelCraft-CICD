package com.example.hot6novelcraft.domain.novel.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.domain.novel.dto.request.NovelCreateRequest;
import com.example.hot6novelcraft.domain.novel.dto.request.NovelUpdateRequest;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelCreateResponse;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelDeleteResponse;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelUpdateResponse;
import com.example.hot6novelcraft.domain.novel.service.NovelService;
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
@RequestMapping("/api/novels")
public class NovelController {

    private final NovelService novelService;

    /**
     * 소설 등록
     * 정은식
     */
    @PostMapping
    public ResponseEntity<BaseResponse<NovelCreateResponse>> createNovel(
            @Valid @RequestBody NovelCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        NovelCreateResponse response = novelService.createNovel(request, userDetails);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("201", "소설 등록 성공", response));
    }

    /**
     * 소설 수정
     * 정은식
     */
    @PatchMapping("/{novelId}")
    public ResponseEntity<BaseResponse<NovelUpdateResponse>> updateNovel(
            @PathVariable Long novelId,
            @Valid @RequestBody NovelUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        NovelUpdateResponse response = novelService.updateNovel(novelId, request, userDetails);

        return ResponseEntity.ok(
                BaseResponse.success("200", "소설 수정 성공", response)
        );
    }

    /**
     * 소설 삭제
     * 정은식
     */
    @DeleteMapping("/{novelId}")
    public ResponseEntity<BaseResponse<NovelDeleteResponse>> deleteNovel(
            @PathVariable Long novelId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        NovelDeleteResponse response = novelService.deleteNovel(novelId, userDetails);

        return ResponseEntity.ok(
                BaseResponse.success("200", "소설 삭제 성공", response)
        );
    }
}