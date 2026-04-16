package com.example.hot6novelcraft.domain.novel.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.common.dto.PageResponse;
import com.example.hot6novelcraft.domain.novel.dto.request.NovelCreateRequest;
import com.example.hot6novelcraft.domain.novel.dto.request.NovelUpdateRequest;
import com.example.hot6novelcraft.domain.novel.dto.response.*;
import com.example.hot6novelcraft.domain.novel.entity.enums.NovelStatus;
import com.example.hot6novelcraft.domain.novel.service.NovelService;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api")
public class NovelController {

    private final NovelService novelService;

    /**
     * 소설 등록
     * 정은식
     */
    @PostMapping("/novels")
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
    @PatchMapping("/novels/{novelId}")
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
    @DeleteMapping("/novels/{novelId}")
    public ResponseEntity<BaseResponse<NovelDeleteResponse>> deleteNovel(
            @PathVariable Long novelId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        NovelDeleteResponse response = novelService.deleteNovel(novelId, userDetails);

        return ResponseEntity.ok(
                BaseResponse.success("200", "소설 삭제 성공", response)
        );
    }

    /**
     * 소설 목록 조회 V1
     * 정은식
     */
    @GetMapping("/v1/novels")
    public ResponseEntity<BaseResponse<PageResponse<NovelListResponse>>> getNovelListV1(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<NovelListResponse> response = novelService.getNovelListV1(pageable);

        return ResponseEntity.ok(
                BaseResponse.success("200", "소설 목록 조회 성공(V1)", response)
        );
    }

    /**
     * 소설 목록 조회 V2
     * 정은식
     */
    @GetMapping("/v2/novels")
    public ResponseEntity<BaseResponse<PageResponse<NovelListResponse>>> getNovelListV2(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) NovelStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<NovelListResponse> response = novelService.getNovelListV2(genre, status, pageable);

        return ResponseEntity.ok(
                BaseResponse.success("200", "소설 목록 조회 성공(V2)", response)
        );
    }

    /**
     * 소설 상세 조회 V1
     * 정은식
     */
    @GetMapping("/v1/novels/{novelId}")
    public ResponseEntity<BaseResponse<NovelDetailResponse>> getNovelDetailV1(
            @PathVariable Long novelId
    ) {
        NovelDetailResponse response = novelService.getNovelDetailV1(novelId);

        return ResponseEntity.ok(
                BaseResponse.success("200", "소설 상세 조회 성공", response)
        );
    }
}