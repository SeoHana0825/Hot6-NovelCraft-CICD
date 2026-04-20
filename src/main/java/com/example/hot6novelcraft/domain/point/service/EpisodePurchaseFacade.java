package com.example.hot6novelcraft.domain.point.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.PaymentExceptionEnum;
import com.example.hot6novelcraft.common.security.RedisUtil;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodePurchaseResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.NovelBulkPurchaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 회차 구매 Facade
 * Redis Lock 관리 담당 (트랜잭션 외부)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EpisodePurchaseFacade {

    private final EpisodePurchaseTransactionService transactionService;
    private final RedisUtil redisUtil;

    /**
     * 회차 단건 구매 (락 관리)
     */
    public EpisodePurchaseResponse purchaseEpisode(Long userId, Long episodeId) {
        log.info("[회차 구매] 요청 userId={} episodeId={}", userId, episodeId);

        // 사용자 단위 락 (단건/전체 구매 레이스 컨디션 방지)
        String lockKey = "purchase:lock:" + userId;
        if (!redisUtil.acquireLock(lockKey)) {
            log.warn("[회차 구매] Lock 획득 실패 (이미 처리 중) userId={}", userId);
            throw new ServiceErrorException(PaymentExceptionEnum.ERR_PAYMENT_PROCESSING);
        }

        try {
            // 트랜잭션 서비스 호출 (커밋까지 완료)
            return transactionService.executePurchase(userId, episodeId);
        } finally {
            // 트랜잭션 커밋 후 락 해제
            redisUtil.releaseLock(lockKey);
        }
    }

    /**
     * 소설 전체 구매 (락 관리)
     */
    public NovelBulkPurchaseResponse purchaseAllEpisodes(Long userId, Long novelId) {
        log.info("[소설 전체 구매] 요청 userId={} novelId={}", userId, novelId);

        // 사용자 단위 락 (단건/전체 구매 레이스 컨디션 방지)
        String lockKey = "purchase:lock:" + userId;
        if (!redisUtil.acquireLock(lockKey)) {
            log.warn("[소설 전체 구매] Lock 획득 실패 (이미 처리 중) userId={}", userId);
            throw new ServiceErrorException(PaymentExceptionEnum.ERR_PAYMENT_PROCESSING);
        }

        try {
            // 트랜잭션 서비스 호출 (커밋까지 완료)
            return transactionService.executeAllPurchase(userId, novelId);
        } finally {
            // 트랜잭션 커밋 후 락 해제
            redisUtil.releaseLock(lockKey);
        }
    }
}
