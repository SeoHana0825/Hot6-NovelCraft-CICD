package com.example.hot6novelcraft.domain.novel.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.NovelExceptionEnum;
import com.example.hot6novelcraft.domain.novel.dto.request.NovelCreateRequest;
import com.example.hot6novelcraft.domain.novel.dto.request.NovelUpdateRequest;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelCreateResponse;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelDeleteResponse;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelUpdateResponse;
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

        // TODO : JWT 구현후 작가ID로 교체 및 작가 권한 확인 예정임다!!!!!!!

        Novel novel = Novel.createNovel(
                1L,
                request.title(),
                request.description(),
                request.genre().toString(),
                request.tagsToString()
        );

        // DB 저장
        Novel savedNovel = novelRepository.save(novel);

        return NovelCreateResponse.from(savedNovel.getId());
    }

    // 소설 수정
    @Transactional
    public NovelUpdateResponse updateNovel(Long novelId, NovelUpdateRequest request) {

        // 소설 조회 공통 메서드
        Novel novel = findNovelById(novelId);

        // TODO : JWT 구현후 작가ID로 교체 및 작가 권한 확인 예정임다!!!!!!!
        // 작가 권한 확인 (role = AUTHOR)
        // 본인 소설 확인 (novel.getAuthorId() == 로그인한 유저 ID)

        // 소설 수정
        novel.update(
                request.title(),
                request.description(),
                request.genre().toString(),
                request.tagsToString()
        );

        return NovelUpdateResponse.from(novel.getId());
    }

    // 소설 삭제
    @Transactional
    public NovelDeleteResponse deleteNovel(Long novelId) {

        // 소설 조회 공통 메서드
        Novel novel = findNovelById(novelId);

        // TODO : JWT 구현후 작가ID로 교체 및 작가 권한 확인 예정임다!!!!!!!
        // 작가 권한 확인 (role = AUTHOR)
        // 본인 소설 확인 (novel.getAuthorId() == 로그인한 유저 ID)

        // 소설 삭제(소프트 딜리트)
        novel.delete();

        return NovelDeleteResponse.from(novel.getId());
    }

    // 소설 조회 공통 메서드
    private Novel findNovelById(Long novelId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new ServiceErrorException(NovelExceptionEnum.NOVEL_NOT_FOUND));

        if (novel.isDeleted()) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_ALREADY_DELETED);
        }

        return novel;
    }
}