package com.example.hot6novelcraft.domain.novel.repository;

import com.example.hot6novelcraft.domain.novel.entity.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NovelRepository extends JpaRepository<Novel, Long>, CustomNovelRepository  {

    // V1 - 소설 목록 조회 (IsDeleted확인)
    Page<Novel> findAllByIsDeletedFalse(Pageable pageable);
}
