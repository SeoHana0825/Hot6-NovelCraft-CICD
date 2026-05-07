package com.example.hot6novelcraft.domain.aichat.loader;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
@DependsOn("vectorStore") // VectorStore 빈이 먼저 생성된 후 실행 보장
public class KnowledgeBaseLoader {

    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;

    private static final List<String> FAQ_FILES = Arrays.asList(
            "classpath:ai/knowledge/faq-payment.md",
            "classpath:ai/knowledge/faq-subscription.md",
            "classpath:ai/knowledge/faq-points.md",
            "classpath:ai/knowledge/faq-mentoring.md",
            "classpath:ai/knowledge/faq-general.md"
    );

    /**
     * 앱 시작 시 FAQ 문서를 VectorStore에 적재한다.
     * SimpleVectorStore는 인메모리이므로 재시작할 때마다 재적재가 필요하다.
     */
    @PostConstruct
    public void load() {
        log.info("FAQ 문서 VectorStore 적재 시작...");

        List<Document> allDocuments = FAQ_FILES.stream()
                .flatMap(this::readDocument)
                .toList();

        // TokenTextSplitter: 토큰 단위로 청크 분할 (기본 800 토큰, 유사도 검색 정확도 향상)
        List<Document> chunks = new TokenTextSplitter().apply(allDocuments);

        vectorStore.add(chunks);

        log.info("FAQ 문서 적재 완료: {}개 파일 → {}개 청크", FAQ_FILES.size(), chunks.size());
    }

    private Stream<Document> readDocument(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                log.warn("FAQ 파일을 찾을 수 없습니다: {}", path);
                return Stream.empty();
            }
            return new TextReader(resource).get().stream();
        } catch (Exception e) {
            log.error("FAQ 파일 읽기 실패: {} - {}", path, e.getMessage());
            return Stream.empty();
        }
    }
}
