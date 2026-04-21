package com.example.hot6novelcraft.domain.novel.repository;

import com.example.hot6novelcraft.domain.novel.entity.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NovelRepository extends JpaRepository<Novel, Long>, CustomNovelRepository  {

    // V1 - 소설 목록 조회 (IsDeleted확인)
    Page<Novel> findAllByIsDeletedFalse(Pageable pageable);

    @Modifying
    @Query("UPDATE Novel n SET n.viewCount = n.viewCount + 1 WHERE n.id = :novelId")
    void incrementViewCount(@Param("novelId") Long novelId);

    // 작가의 삭제되지 않은 소설 ID 목록 조회
    @Query("SELECT n.id FROM Novel n WHERE n.authorId = :authorId AND n.isDeleted = false")
    List<Long> findNovelIdsByAuthorId(@Param("authorId") Long authorId);

    // V2: soft-delete 조건 포함 조회 — findById 대신 사용
    Optional<Novel> findByIdAndIsDeletedFalse(Long id);
}
