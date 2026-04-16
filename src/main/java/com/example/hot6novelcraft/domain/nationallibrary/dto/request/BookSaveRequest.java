package com.example.hot6novelcraft.domain.nationallibrary.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BookSaveRequest(
        @NotBlank(message = "ISBN을 입력해 주세요")
        String isbn,

        @NotBlank(message = "제목을 입력해 주세요")
        String title,

        @NotBlank(message = "저자를 입력해 주세요")
        String author,

        @NotBlank(message = "출판사를 입력해 주세요")
        String publisher,

        @NotBlank(message = "출판년도를 입력해 주세요")
        String publishYear,

        String coverImageUrl
) {}
