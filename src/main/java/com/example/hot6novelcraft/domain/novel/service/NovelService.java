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

import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NovelService {

    private final NovelRepository novelRepository;

    // 소설 등록
    @Transactional
    public NovelCreateResponse createNovel(NovelCreateRequest request, UserDetailsImpl userDetails) {

        User user = userDetails.getUser();

        // 작가 권한 확인
        if (user.getRole() != UserRole.AUTHOR) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_AUTHOR_FORBIDDEN);
        }

        Novel novel = Novel.createNovel(
                user.getId(),
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
    public NovelUpdateResponse updateNovel(Long novelId, NovelUpdateRequest request, UserDetailsImpl userDetails) {

        User user = userDetails.getUser();

        // 작가 권한 확인
        if (user.getRole() != UserRole.AUTHOR) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_AUTHOR_FORBIDDEN);
        }

        // 소설 조회 공통 메서드(본인 소설 및 삭제여부)
        Novel novel = findNovelById(novelId, user.getId());


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
    public NovelDeleteResponse deleteNovel(Long novelId, UserDetailsImpl userDetails) {

        User user = userDetails.getUser();

        // 작가 권한 확인
        if (user.getRole() != UserRole.AUTHOR) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_AUTHOR_FORBIDDEN);
        }

        // 소설 조회 공통 메서드(본인 소설 및 삭제여부)
        Novel novel = findNovelById(novelId, user.getId());

        // 소설 삭제(소프트 딜리트)
        novel.delete();

        return NovelDeleteResponse.from(novel.getId());
    }

    // 소설 조회 공통 메서드(본인 소설 및 삭제여부)
    private Novel findNovelById(Long novelId, Long userId) {

        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new ServiceErrorException(NovelExceptionEnum.NOVEL_NOT_FOUND));

        // 본인 소설 확인 먼저
        if (!Objects.equals(novel.getAuthorId(), userId)) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_FORBIDDEN);
        }

        // 삭제 여부
        if (novel.isDeleted()) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_ALREADY_DELETED);
        }

        return novel;
    }
}