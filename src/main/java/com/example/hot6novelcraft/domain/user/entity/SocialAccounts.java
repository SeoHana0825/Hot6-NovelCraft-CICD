package com.example.hot6novelcraft.domain.user.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "social_accounts")
public class SocialAccounts extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProviderSns provider;

    private SocialAccounts(Long userId, ProviderSns provider) {
        this.userId = userId;
        this.provider = provider;
    }

    @Builder
    public static SocialAccounts register(Long userId, ProviderSns provider) {
      return new SocialAccounts(userId, provider);
    }

}
