package com.example.hot6novelcraft.domain.novel.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.domain.novel.dto.request.NovelCreateRequest;
import com.example.hot6novelcraft.domain.novel.dto.request.NovelUpdateRequest;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelCreateResponse;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelDeleteResponse;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelUpdateResponse;
import com.example.hot6novelcraft.domain.novel.entity.enums.MainGenre;
import com.example.hot6novelcraft.domain.novel.entity.Novel;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import com.example.hot6novelcraft.domain.user.entity.userEnum.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class NovelServiceTest {

    @Mock
    NovelRepository novelRepository;

    @InjectMocks
    NovelService novelService;

    // 작가 Mock
    private UserDetailsImpl 작가() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(user.getRole()).willReturn(UserRole.AUTHOR);

        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        given(userDetails.getUser()).willReturn(user);
        return userDetails;
    }

    // 독자 Mock (작가 권한 없음)
    private UserDetailsImpl 독자() {
        User user = mock(User.class);
        given(user.getId()).willReturn(2L);
        given(user.getRole()).willReturn(UserRole.READER);

        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        given(userDetails.getUser()).willReturn(user);
        return userDetails;
    }

    // 소설 등록 요청 Mock
    private NovelCreateRequest 소설등록요청() {
        NovelCreateRequest request = mock(NovelCreateRequest.class);
        given(request.title()).willReturn("테스트 소설");
        given(request.description()).willReturn("소설 소개");
        given(request.genre()).willReturn(MainGenre.FANTASY);
        given(request.tagsToString()).willReturn("ROMANCE,ACTION");
        return request;
    }

    // 소설 엔티티 Mock
    private Novel 소설(Long authorId) {
        Novel novel = mock(Novel.class);
        given(novel.getId()).willReturn(1L);
        given(novel.getAuthorId()).willReturn(authorId);
        given(novel.isDeleted()).willReturn(false);
        return novel;
    }

    // ==================== 소설 등록 ====================

    @Test
    void 소설등록_작가권한이면_성공() {
        UserDetailsImpl userDetails = 작가();
        NovelCreateRequest request = 소설등록요청();
        Novel novel = 소설(1L);

        given(novelRepository.save(any())).willReturn(novel);

        NovelCreateResponse response = novelService.createNovel(request, userDetails);

        assertEquals(novel.getId(), response.novelId());
    }

    @Test
    void 소설등록_작가권한없으면_실패() {
        UserDetailsImpl userDetails = 독자();
        NovelCreateRequest request = 소설등록요청();

        assertThrows(ServiceErrorException.class,
                () -> novelService.createNovel(request, userDetails));
    }

    // ==================== 소설 수정 ====================

    @Test
    void 소설수정_본인소설이면_성공() {
        UserDetailsImpl userDetails = 작가();
        Novel novel = 소설(1L);

        NovelUpdateRequest request = mock(NovelUpdateRequest.class);
        given(request.title()).willReturn("수정된 제목");
        given(request.description()).willReturn("수정된 소개");
        given(request.genre()).willReturn(MainGenre.FANTASY);
        given(request.tagsToString()).willReturn("ROMANCE");

        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));

        NovelUpdateResponse response = novelService.updateNovel(1L, request, userDetails);

        assertEquals(novel.getId(), response.novelId());
    }

    @Test
    void 소설수정_작가권한없으면_실패() {
        UserDetailsImpl userDetails = 독자();
        NovelUpdateRequest request = mock(NovelUpdateRequest.class);

        assertThrows(ServiceErrorException.class,
                () -> novelService.updateNovel(1L, request, userDetails));
    }

    @Test
    void 소설수정_소설없으면_실패() {
        UserDetailsImpl userDetails = 작가();
        NovelUpdateRequest request = mock(NovelUpdateRequest.class);

        given(novelRepository.findById(1L)).willReturn(Optional.empty());

        assertThrows(ServiceErrorException.class,
                () -> novelService.updateNovel(1L, request, userDetails));
    }

    @Test
    void 소설수정_본인소설아니면_실패() {
        UserDetailsImpl userDetails = 작가(); // userId = 1L
        Novel novel = 소설(2L); // authorId = 2L (다른 작가)

        NovelUpdateRequest request = mock(NovelUpdateRequest.class);
        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));

        assertThrows(ServiceErrorException.class,
                () -> novelService.updateNovel(1L, request, userDetails));
    }

    @Test
    void 소설수정_이미삭제된소설이면_실패() {
        UserDetailsImpl userDetails = 작가();
        Novel novel = mock(Novel.class);
        given(novel.getAuthorId()).willReturn(1L);
        given(novel.isDeleted()).willReturn(true);

        NovelUpdateRequest request = mock(NovelUpdateRequest.class);
        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));

        assertThrows(ServiceErrorException.class,
                () -> novelService.updateNovel(1L, request, userDetails));
    }

    // ==================== 소설 삭제 ====================

    @Test
    void 소설삭제_본인소설이면_성공() {
        UserDetailsImpl userDetails = 작가();
        Novel novel = 소설(1L);

        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));

        NovelDeleteResponse response = novelService.deleteNovel(1L, userDetails);

        assertEquals(novel.getId(), response.novelId());
    }

    @Test
    void 소설삭제_작가권한없으면_실패() {
        UserDetailsImpl userDetails = 독자();

        assertThrows(ServiceErrorException.class,
                () -> novelService.deleteNovel(1L, userDetails));
    }

    @Test
    void 소설삭제_본인소설아니면_실패() {
        UserDetailsImpl userDetails = 작가(); // userId = 1L
        Novel novel = 소설(2L); // authorId = 2L

        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));

        assertThrows(ServiceErrorException.class,
                () -> novelService.deleteNovel(1L, userDetails));
    }

    @Test
    void 소설삭제_이미삭제된소설이면_실패() {
        UserDetailsImpl userDetails = 작가();
        Novel novel = mock(Novel.class);
        given(novel.getAuthorId()).willReturn(1L);
        given(novel.isDeleted()).willReturn(true);

        given(novelRepository.findById(1L)).willReturn(Optional.of(novel));

        assertThrows(ServiceErrorException.class,
                () -> novelService.deleteNovel(1L, userDetails));
    }
}