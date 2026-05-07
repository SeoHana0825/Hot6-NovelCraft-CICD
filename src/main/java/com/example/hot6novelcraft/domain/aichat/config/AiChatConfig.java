package com.example.hot6novelcraft.domain.aichat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatConfig {

    @Bean
    public ChatClient customerServiceChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4.1-nano")
                        .build())
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                .defaultSystem("""
                        너는 NovelCraft 고객센터 AI다.
                        다음 규칙을 반드시 지켜라:
                        1. 반드시 제공된 문서 기반으로만 답변한다.
                        2. 문서에 없으면 "담당자에게 문의해주세요"라고 말한다.
                        3. 답변은 친절하고 짧게 (3~5줄)
                        4. 불필요한 설명 금지
                        5. 한국어로만 답변
                        """)
                .build();
    }
}
