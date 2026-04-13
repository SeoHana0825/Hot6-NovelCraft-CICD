package com.example.hot6novelcraft.domain.user.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "reader_profiles")
public class ReaderProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String preferred_genres;

    @Enumerated(EnumType.STRING)
    private ReadingGoal readingGoal;
}
