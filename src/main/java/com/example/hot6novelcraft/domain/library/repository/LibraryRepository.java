package com.example.hot6novelcraft.domain.library.repository;

import com.example.hot6novelcraft.domain.library.entity.Library;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import com.example.hot6novelcraft.domain.library.entity.Library;
import com.example.hot6novelcraft.domain.library.entity.enums.LibraryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LibraryRepository extends JpaRepository<Library, Long> {

    Optional<Library> findByUserIdAndNovelId(Long userId, Long novelId);

    boolean existsByUserIdAndNovelId(Long userId, Long novelId);
}