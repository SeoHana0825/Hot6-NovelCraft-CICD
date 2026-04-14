package com.example.hot6novelcraft.domain.novel.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.domain.novel.dto.request.NovelCreateRequest;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelCreateResponse;
import com.example.hot6novelcraft.domain.novel.service.NovelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/novels")
public class NovelController {

    private final NovelService novelService;

    /**
     * 소설 등록
     * 정은식
     */
    @PostMapping
    public ResponseEntity<BaseResponse<NovelCreateResponse>> createNovel(
            @Valid @RequestBody NovelCreateRequest request
    ) {
        NovelCreateResponse response = novelService.createNovel(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("201", "소설 등록 성공", response));
    }
}