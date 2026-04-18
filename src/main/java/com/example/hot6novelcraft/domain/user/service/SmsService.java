package com.example.hot6novelcraft.domain.user.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.UserExceptionEnum;
import com.example.hot6novelcraft.common.security.RedisUtil;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Random;

@Slf4j(topic = "SmsService")
@Service
public class SmsService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final DefaultMessageService messageService;
    private final RedisUtil redisUtil;
    private String fromNumber;
    private final boolean isTestMode;

    public SmsService(
            @Value("${coolsms.api-key}") String apiKey
            , @Value("${coolsms.secret-key}") String apiSecret
            , @Value("${coolsms.send-number}") String fromNumber
            , @Value("${coolsms.test-mode}") boolean isTestMode
            , RedisUtil redisUtil
    ) {
        // 4.x 버전 - 여기서 CoolSMS 객체 초기화
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey,apiSecret,"https://api.coolsms.co.kr");
        this.fromNumber = fromNumber;
        this.redisUtil = redisUtil;
        this.isTestMode = isTestMode;
    }

    private String createRandomCode() {
        StringBuilder randomCode = new StringBuilder(6);

        for (int i = 0; i < 6; i++) {
            randomCode.append(RANDOM.nextInt(10));
        }
        return randomCode.toString();
    }

    // 인증번호 전송
    public void sendSMS(String phoneNumber) {

        // 랜덤한 인증번호 생성
        String randomCode = createRandomCode();

        // 인증 번호 redis 저장 (TTL 5분)
        String redisKey = "SMS:VERIFIED:" + phoneNumber;
        redisUtil.set(redisKey, randomCode, 5);

        // testMode 켜져있을 때 로그만 찍힘
        if(isTestMode) {
            log.info("[SMS TEST] 진짜 문자를 발송하지 않습니다.");
            log.info("[SMS TEST] 수신번호: {}, 인증번호: [{}]", phoneNumber, randomCode);
            return;
        }

        // 발신 정보 설정
        Message message = new Message();
        message.setFrom(fromNumber);
        message.setTo(phoneNumber);
        message.setText("[NovelCraft] 인증번호: [" + randomCode + "]을 입력해 주세요. 인증번호가 타인에게 유출되지 않도록 주의해주시길 바랍니다.");

        try {
            SingleMessageSentResponse response = this.messageService.sendOne(
                    new SingleMessageSendingRequest(message)
            );
            log.info("[SMS] 전송 성공, 결과: {}", response);

        } catch (Exception e) {
            log.error("[SMS] 전송 실패", e);
            throw new ServiceErrorException(UserExceptionEnum.ERR_FAILED_SEND_SMS);
        }

    }

    // 인증번호 검증 및 예외 처리
    public boolean verifyAuthCode(String phoneNumber, String inputCode) {
        String redisKey = "SMS:VERIFIED:" + phoneNumber;
        Object storedCode = redisUtil.get(redisKey);

        // 만료 체크 : Redis에 값이 없을 떄 (TTL 5분)
        if(storedCode == null) {
            log.warn("[SMS] 인증번호 만료, key: {}", redisKey);
            throw new ServiceErrorException(UserExceptionEnum.ERR_INVALID_PHONE);
        }

        // 일치 여부 체크
        if(!storedCode.toString().equals(inputCode)) {
            log.error("[SMS] 인증번호 불일치, key: {}", redisKey);
            throw new ServiceErrorException(UserExceptionEnum.ERR_INVALID_PHONE_VERIFICATION);
        }

        // 검증 성공 - 즉시 삭제
        redisUtil.delete(redisKey);

        String verifiedKey = "SMS:VERIFIED:" + phoneNumber;
        redisUtil.set(verifiedKey, "TRUE", 10);
        
        log.info("[SMS] 인증번호 검증 성공, key: {}", redisKey);
        return true;
    }

}
