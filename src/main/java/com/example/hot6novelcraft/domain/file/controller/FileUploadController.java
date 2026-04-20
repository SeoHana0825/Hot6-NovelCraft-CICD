package com.example.hot6novelcraft.domain.file.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.domain.file.dto.response.FileUploadResponse;
import com.example.hot6novelcraft.domain.file.service.FileUploadService;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * 채팅 첨부 파일 업로드
     * - 최대 10MB, 이미지/문서만 허용
     * - S3 업로드 후 URL 반환
     */
    @PostMapping("/chat")
    public BaseResponse<FileUploadResponse> uploadChatFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getUser().getId();
        log.info("[FileUpload] 파일 업로드 요청 userId={} filename={}", userId, file.getOriginalFilename());

        String fileUrl = fileUploadService.uploadChatFile(file);
        return BaseResponse.success("200", "파일 업로드 완료", new FileUploadResponse(fileUrl));
    }
}
