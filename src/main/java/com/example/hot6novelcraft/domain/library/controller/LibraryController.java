package com.example.hot6novelcraft.domain.library.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.common.dto.PageResponse;
import com.example.hot6novelcraft.domain.library.dto.request.LibraryAddRequest;
import com.example.hot6novelcraft.domain.library.dto.response.LibraryAddResponse;
import com.example.hot6novelcraft.domain.library.dto.response.LibraryListResponse;
import com.example.hot6novelcraft.domain.library.entity.enums.LibraryType;
import com.example.hot6novelcraft.domain.library.service.LibraryService;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/libraries")
@RequiredArgsConstructor
@Validated
public class LibraryController {

    private final LibraryService libraryService;

    // 내서재 담기 (찜/구매/읽는중 모두 이 API 사용)
    @PostMapping("/{id}")
    public ResponseEntity<BaseResponse<LibraryAddResponse>> addToLibrary(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody LibraryAddRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        LibraryAddResponse response = libraryService.addToLibrary(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success("201", "소설을 서재에 성공적으로 담았습니다", response));
    }

    // 내서재 목록 조회
    // type 미입력 시 전체 조회, 입력 시 해당 타입만 조회
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<PageResponse<LibraryListResponse>>> getMyLibrary(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) LibraryType libraryType,
            @RequestParam(defaultValue = "1")  @Min(value = 1, message = "페이지는 1 이상이어야 합니다") int page,
            @RequestParam(defaultValue = "12") @Min(value = 1, message = "사이즈는 1 이상이어야 합니다")
            @Max(value = 100, message = "사이즈는 100 이하여야 합니다") int size,
            @RequestParam(defaultValue = "LATEST") String sort
    ) {
        Long userId = userDetails.getUser().getId();

        Page<LibraryListResponse> result =
                libraryService.getMyLibrary(userId, libraryType, page - 1, size, sort);

        return ResponseEntity.ok(
                BaseResponse.success("200", "내 서재 목록 조회가 완료되었습니다",
                        PageResponse.register(result))
        );
    }
}