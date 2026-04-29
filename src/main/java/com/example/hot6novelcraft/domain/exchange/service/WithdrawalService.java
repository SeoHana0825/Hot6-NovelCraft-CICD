package com.example.hot6novelcraft.domain.exchange.service;

import com.example.hot6novelcraft.common.dto.PageResponse;
import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.domain.exchange.dto.request.WithdrawalCreateRequest;
import com.example.hot6novelcraft.domain.exchange.dto.response.BankAccountInfoResponse;
import com.example.hot6novelcraft.domain.exchange.dto.response.WithdrawalDetailResponse;
import com.example.hot6novelcraft.domain.exchange.dto.response.WithdrawalResponse;
import com.example.hot6novelcraft.domain.exchange.entity.BankAccount;
import com.example.hot6novelcraft.domain.exchange.entity.Revenue;
import com.example.hot6novelcraft.domain.exchange.entity.Withdrawal;
import com.example.hot6novelcraft.domain.exchange.entity.enums.FeePolicy;
import com.example.hot6novelcraft.domain.exchange.entity.enums.RevenueType;
import com.example.hot6novelcraft.domain.exchange.entity.enums.WithdrawalStatus;
import com.example.hot6novelcraft.domain.exchange.exception.ExchangeExceptionEnum;
import com.example.hot6novelcraft.domain.exchange.repository.BankAccountRepository;
import com.example.hot6novelcraft.domain.exchange.repository.RevenueRepository;
import com.example.hot6novelcraft.domain.exchange.repository.WithdrawalRepository;
import com.example.hot6novelcraft.domain.exchange.util.AesEncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WithdrawalService {

    private static final String WITHDRAWAL_LOCK_PREFIX = "lock:withdrawal:";
    private static final long LOCK_TIMEOUT_SECONDS = 5;

    private final WithdrawalRepository withdrawalRepository;
    private final RevenueRepository revenueRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RevenueService revenueService;

    /**
     * 환전 신청
     * 1. Redis 분산락 획득 (동시 환전 방지)
     * 2. 인증된 계좌 확인
     * 3. PENDING 상태 환전 건 중복 확인
     * 4. 최소 금액 검증
     * 5. 잔액 검증
     * 6. 수수료 계산
     * 7. Revenue(WITHDRAWAL) + Withdrawal 생성 (한 트랜잭션)
     * 8. 캐시 무효화
     */
    @Transactional
    public WithdrawalResponse createWithdrawal(Long authorId, WithdrawalCreateRequest request) {
        String lockKey = WITHDRAWAL_LOCK_PREFIX + authorId;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(acquired)) {
            throw new ServiceErrorException(ExchangeExceptionEnum.WITHDRAWAL_PENDING_EXISTS);
        }

        try {
            return processWithdrawal(authorId, request);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private WithdrawalResponse processWithdrawal(Long authorId, WithdrawalCreateRequest request) {
        // 인증된 계좌 확인
        BankAccount bankAccount = bankAccountRepository.findByUserIdAndIsVerifiedTrue(authorId)
                .orElseThrow(() -> new ServiceErrorException(ExchangeExceptionEnum.BANK_ACCOUNT_NOT_VERIFIED));

        // PENDING 상태 환전 건 중복 확인
        if (withdrawalRepository.existsByAuthorIdAndStatus(authorId, WithdrawalStatus.PENDING)) {
            throw new ServiceErrorException(ExchangeExceptionEnum.WITHDRAWAL_PENDING_EXISTS);
        }

        // 최소 금액 검증
        FeePolicy feePolicy = FeePolicy.DEFAULT;
        if (request.requestAmount() < feePolicy.getMinimumWithdrawalAmount()) {
            throw new ServiceErrorException(ExchangeExceptionEnum.WITHDRAWAL_BELOW_MINIMUM);
        }

        // 잔액 검증
        Integer totalEarned = revenueRepository.sumAmountByAuthorIdAndTypeIn(
                authorId, List.of(RevenueType.EPISODE_SALE, RevenueType.SUBSCRIPTION)
        );
        Integer totalWithdrawn = revenueRepository.sumAmountByAuthorIdAndType(
                authorId, RevenueType.WITHDRAWAL
        );
        Integer availableBalance = totalEarned - totalWithdrawn;

        if (availableBalance < request.requestAmount()) {
            throw new ServiceErrorException(ExchangeExceptionEnum.WITHDRAWAL_INSUFFICIENT_BALANCE);
        }

        // 수수료 계산
        int fee = feePolicy.calculateFee(request.requestAmount());
        int balanceAfter = availableBalance - request.requestAmount();

        // Revenue(WITHDRAWAL) 차감 기록
        Revenue withdrawalRevenue = Revenue.ofWithdrawal(authorId, request.requestAmount(), balanceAfter);
        revenueRepository.save(withdrawalRevenue);

        // Withdrawal 생성
        Withdrawal withdrawal = Withdrawal.request(authorId, bankAccount.getId(), request.requestAmount(), fee);
        withdrawalRepository.save(withdrawal);

        // 캐시 무효화
        revenueService.evictRevenueOverviewCache(authorId);

        log.info("환전 신청 완료 - authorId: {}, 금액: {}, 수수료: {}, 실수령: {}",
                authorId, request.requestAmount(), fee, withdrawal.getActualAmount());

        return WithdrawalResponse.from(withdrawal);
    }

    /**
     * 환전 내역 목록 조회 (기간 필터 + 상태 필터 + 페이징)
     */
    public PageResponse<WithdrawalResponse> getWithdrawals(
            Long authorId,
            WithdrawalStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        Page<Withdrawal> page = withdrawalRepository.findWithFilters(
                authorId, status, startDate, endDate, pageable
        );

        Page<WithdrawalResponse> responsePage = page.map(WithdrawalResponse::from);
        return PageResponse.register(responsePage);
    }

    /**
     * 환전 상세 조회
     */
    public WithdrawalDetailResponse getWithdrawalDetail(Long authorId, Long withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findByIdAndAuthorId(withdrawalId, authorId)
                .orElseThrow(() -> new ServiceErrorException(ExchangeExceptionEnum.WITHDRAWAL_NOT_FOUND));

        // 계좌 정보 조회
        BankAccount bankAccount = bankAccountRepository.findById(withdrawal.getBankAccountId())
                .orElseThrow(() -> new ServiceErrorException(ExchangeExceptionEnum.BANK_ACCOUNT_NOT_FOUND));

        String decryptedNumber = aesEncryptionUtil.decrypt(bankAccount.getAccountNumber());
        String maskedNumber = bankAccount.getMaskedAccountNumber(decryptedNumber);

        BankAccountInfoResponse bankAccountInfo = BankAccountInfoResponse.of(
                bankAccount.getId(),
                bankAccount.getBankName(),
                maskedNumber,
                bankAccount.getAccountHolder(),
                bankAccount.getIsVerified()
        );

        return WithdrawalDetailResponse.of(withdrawal, bankAccountInfo);
    }

    // ========================================================================
    // TODO: 관리자 도메인 개발 완료 후 Admin Controller에서 호출
    // - PUT /api/admin/exchanges/{id}/approve → approveWithdrawal()
    // - PUT /api/admin/exchanges/{id}/reject  → rejectWithdrawal()
    // ========================================================================

    /**
     * [관리자] 환전 승인
     * - PENDING → COMPLETED 상태 변경
     * - processedAt 기록
     * - 실제 송금 처리 (현재는 로그만 기록)
     */
    @Transactional
    public WithdrawalResponse approveWithdrawal(Long withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ServiceErrorException(ExchangeExceptionEnum.WITHDRAWAL_NOT_FOUND));

        withdrawal.complete();

        // TODO: 실제 은행 API를 통한 송금 처리
        log.info("환전 승인 완료 - withdrawalId: {}, authorId: {}, 실수령액: {}",
                withdrawalId, withdrawal.getAuthorId(), withdrawal.getActualAmount());

        // 캐시 무효화
        revenueService.evictRevenueOverviewCache(withdrawal.getAuthorId());

        return WithdrawalResponse.from(withdrawal);
    }

    /**
     * [관리자] 환전 거절
     * - PENDING → REJECTED 상태 변경 + 거절 사유 기록
     * - 차감된 잔액 복구 (Revenue 복구 건 생성)
     */
    @Transactional
    public WithdrawalResponse rejectWithdrawal(Long withdrawalId, String rejectedReason) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ServiceErrorException(ExchangeExceptionEnum.WITHDRAWAL_NOT_FOUND));

        withdrawal.reject(rejectedReason);

        // 잔액 복구: 차감했던 금액만큼 다시 돌려주는 Revenue 생성
        Integer totalEarned = revenueRepository.sumAmountByAuthorIdAndTypeIn(
                withdrawal.getAuthorId(), List.of(RevenueType.EPISODE_SALE, RevenueType.SUBSCRIPTION)
        );
        Integer totalWithdrawn = revenueRepository.sumAmountByAuthorIdAndType(
                withdrawal.getAuthorId(), RevenueType.WITHDRAWAL
        );
        Integer currentBalance = totalEarned - totalWithdrawn;
        Integer restoredBalance = currentBalance + withdrawal.getRequestAmount();

        Revenue refundRevenue = Revenue.create(
                withdrawal.getAuthorId(),
                null,
                withdrawal.getRequestAmount(),
                restoredBalance,
                RevenueType.WITHDRAWAL  // 음의 차감 복구 (잔액이 증가하는 방향)
        );
        revenueRepository.save(refundRevenue);

        log.info("환전 거절 완료 - withdrawalId: {}, authorId: {}, 복구금액: {}, 사유: {}",
                withdrawalId, withdrawal.getAuthorId(), withdrawal.getRequestAmount(), rejectedReason);

        // 캐시 무효화
        revenueService.evictRevenueOverviewCache(withdrawal.getAuthorId());

        return WithdrawalResponse.from(withdrawal);
    }
}