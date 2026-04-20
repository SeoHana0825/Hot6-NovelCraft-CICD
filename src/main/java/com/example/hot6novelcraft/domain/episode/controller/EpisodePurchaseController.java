package com.example.hot6novelcraft.domain.episode.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodePurchaseResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.NovelBulkPurchaseResponse;
import com.example.hot6novelcraft.domain.point.service.EpisodePurchaseFacade;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api")
public class EpisodePurchaseController {

    private final EpisodePurchaseFacade purchaseFacade;

    /**
     * 회차 단건 구매
     */
    @PostMapping("/episodes/{episodeId}/purchase")
    public ResponseEntity<BaseResponse<EpisodePurchaseResponse>> purchaseEpisode(
            @PathVariable Long episodeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        EpisodePurchaseResponse response =
                purchaseFacade.purchaseEpisode(userDetails.getUser().getId(), episodeId);

        return ResponseEntity.ok(
                BaseResponse.success("200", "회차 구매 성공", response)
        );
    }

    /**
     * 소설 전체 구매
     */
    @PostMapping("/novels/{novelId}/episodes/purchase")
    public ResponseEntity<BaseResponse<NovelBulkPurchaseResponse>> purchaseAllEpisodes(
            @PathVariable Long novelId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        NovelBulkPurchaseResponse response =
                purchaseFacade.purchaseAllEpisodes(userDetails.getUser().getId(), novelId);

        return ResponseEntity.ok(
                BaseResponse.success("200", "소설 전체 구매 성공", response)
        );
    }
}