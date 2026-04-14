package com.example.hot6novelcraft.domain.novel.service;

import com.example.hot6novelcraft.domain.novel.dto.request.NovelCreateRequest;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelCreateResponse;
import com.example.hot6novelcraft.domain.novel.entity.Novel;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NovelService {

    private final NovelRepository novelRepository;

    // 소설 등록
    @Transactional
    public NovelCreateResponse createNovel(NovelCreateRequest request) {

        // JWT 구현후 작가ID로 교체 및 작가 권한 확인 예정임다!!!!!!!

        Novel novel = Novel.builder()
                .authorId(1L) // 임시 JWT 구현 후 수정
                .title(request.title())
                .description(request.description())
                .genre(request.genre().toString())
                .tags(request.tagsToString())
                .build();
        // DB 저장
        Novel savedNovel = novelRepository.save(novel);

        return NovelCreateResponse.from(savedNovel.getId());
    }
}