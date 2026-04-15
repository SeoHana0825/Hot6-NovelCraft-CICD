package com.example.hot6novelcraft.domain.calendar.repository;

import com.example.hot6novelcraft.domain.calendar.entity.Library;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LibraryRepository extends JpaRepository<Library, Long> {
    Optional<Library> findByUserIdAndNovelId(Long userId, Long novelId);
}
