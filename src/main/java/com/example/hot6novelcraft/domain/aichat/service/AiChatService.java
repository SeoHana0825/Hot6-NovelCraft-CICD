package com.example.hot6novelcraft.domain.aichat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient customerServiceChatClient;

    public Flux<String> chat(Long userId, String message) {
        // 1단계: 시스템 프롬프트만으로 기본 동작 확인
        // conversationId는 2단계(대화 메모리 연결)에서 활성화
        return customerServiceChatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

    public void clearSession(Long userId) {
        // 2단계(대화 메모리 연결)에서 구현 예정
    }
}