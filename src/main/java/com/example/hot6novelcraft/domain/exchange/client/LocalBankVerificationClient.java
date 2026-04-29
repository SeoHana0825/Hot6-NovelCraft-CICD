package com.example.hot6novelcraft.domain.exchange.client;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.domain.exchange.exception.ExchangeExceptionEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1원 계좌 인증 시뮬레이션 구현체
 * <p>
 * 금융규제로 인해 사업자 등록 없이는 실제 은행 API(useB, CODEF 등) 연동이 불가능하므로,
 * 실제 인증 플로우와 동일한 비즈니스 로직을 자체적으로 시뮬레이션합니다.
 * <p>
 * 운영 환경에서는 이 클래스 대신 BankVerificationClient 인터페이스를 구현한
 * 실제 API 연동 구현체(예: UseBVerificationClient)로 교체하면 됩니다.
 */
@Slf4j
@Component
public class LocalBankVerificationClient implements BankVerificationClient {

    private static final Random RANDOM = new Random();

    /**
     * 시뮬레이션용 가상 계좌 데이터
     * - 실제 서비스에서는 은행 API가 예금주를 반환하지만,
     *   시뮬레이션에서는 사전 등록된 계좌 정보로 검증합니다.
     * - key: "은행명:계좌번호", value: 예금주명
     */
    private static final Map<String, String> VIRTUAL_ACCOUNTS = Map.of(
            "국민은행:1234567890", "홍길동",
            "신한은행:9876543210", "김철수",
            "우리은행:1111222233", "이영희",
            "하나은행:4444555566", "박민수",
            "카카오뱅크:3333444455", "최지은"
    );

    /**
     * 동적으로 추가되는 계좌 (테스트 유연성을 위해)
     */
    private final Map<String, String> dynamicAccounts = new ConcurrentHashMap<>();

    @Override
    public String verifyAccountOwner(String bankName, String accountNumber) {
        log.info("[Local] 예금주 확인 요청 - 은행: {}, 계좌: {}", bankName, maskAccount(accountNumber));

        String key = bankName + ":" + accountNumber;

        // 1. 사전 등록된 가상 계좌에서 조회
        String holder = VIRTUAL_ACCOUNTS.get(key);

        // 2. 동적 등록 계좌에서 조회
        if (holder == null) {
            holder = dynamicAccounts.get(key);
        }

        // 3. 등록되지 않은 계좌 → 실제 은행 API에서도 존재하지 않는 계좌면 에러
        if (holder == null) {
            log.warn("[Local] 등록되지 않은 가상 계좌 - {}", key);
            throw new ServiceErrorException(ExchangeExceptionEnum.BANK_API_CALL_FAILED);
        }

        log.info("[Local] 예금주 확인 완료 - 예금주: {}", holder);
        return holder;
    }

    @Override
    public String requestOneWonTransfer(String bankName, String accountNumber) {
        String code = String.format("%04d", RANDOM.nextInt(10000));

        log.info("[Local] 1원 입금 시뮬레이션 - 은행: {}, 계좌: {}, 입금자명: 노벨크래프트{}",
                bankName, maskAccount(accountNumber), code);
        log.info("[Local] ※ 실제 환경에서는 이 코드가 고객 통장에 '노벨크래프트{}' 형태로 표시됩니다", code);

        return code;
    }

    @Override
    public boolean isBankMaintenanceTime() {
        LocalTime now = LocalTime.now();
        // 실제 은행 점검시간: 23:30 ~ 00:30
        boolean isMaintenance = now.isAfter(LocalTime.of(23, 30)) || now.isBefore(LocalTime.of(0, 30));

        if (isMaintenance) {
            log.info("[Local] 은행 점검시간 - 현재: {}", now);
        }

        return isMaintenance;
    }

    /**
     * 테스트용 가상 계좌 동적 등록
     * - 시연 또는 테스트 시 원하는 계좌를 추가할 수 있습니다.
     */
    public void registerVirtualAccount(String bankName, String accountNumber, String accountHolder) {
        String key = bankName + ":" + accountNumber;
        dynamicAccounts.put(key, accountHolder);
        log.info("[Local] 가상 계좌 등록 - 은행: {}, 계좌: {}, 예금주: {}", bankName, maskAccount(accountNumber), accountHolder);
    }

    private String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "*".repeat(accountNumber.length() - 4)
                + accountNumber.substring(accountNumber.length() - 4);
    }
}