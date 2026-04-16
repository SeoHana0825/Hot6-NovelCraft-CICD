package com.example.hot6novelcraft.domain.nationallibrary.infrastructure;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.NationalLibraryExceptionEnum;
import com.example.hot6novelcraft.domain.nationallibrary.dto.response.NationalLibraryApiItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NationalLibraryApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${nationallibrary.api.key}")
    private String apiKey;

    @Value("${nationallibrary.api.url}")
    private String apiUrl;

    private static final String NL_BASE_URL = "https://www.nl.go.kr";

    public List<NationalLibraryApiItem> searchBooks(String query, int page, int size) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("key", apiKey)
                    .queryParam("kwd", query)
                    .queryParam("pageNum", page)
                    .queryParam("pageSize", size)
                    .queryParam("srchTarget", "total")
                    .queryParam("apiType", "json")
                    .build()
                    .encode()
                    .toUri();

            log.info("국립중앙도서관 API 호출 URI: {}", uri);

            String rawResponse = restTemplate.getForObject(uri, String.class);
            log.info("국립중앙도서관 API 원본 응답: {}", rawResponse);

            if (rawResponse == null || rawResponse.isBlank()) {
                return List.of();
            }

            NationalLibraryApiResponse response =
                    objectMapper.readValue(rawResponse, NationalLibraryApiResponse.class);

            if (response == null || response.result() == null) {
                return List.of();
            }

            return response.result().stream()
                    .map(item -> new NationalLibraryApiItem(
                            item.isbn(),
                            stripHtml(item.title()),
                            item.author(),
                            item.publisher(),
                            item.publishYear(),
                            resolveUrl(item.titleUrl()),
                            item.imageUrl()
                    ))
                    .toList();

        } catch (Exception e) {
            log.error("국립중앙도서관 API 호출 실패: {}", e.getMessage());
            throw new ServiceErrorException(NationalLibraryExceptionEnum.EXTERNAL_API_ERROR);
        }
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").trim();
    }

    private String resolveUrl(String url) {
        if (url == null || url.isBlank()) return "";
        return url.startsWith("/") ? NL_BASE_URL + url : url;
    }
}