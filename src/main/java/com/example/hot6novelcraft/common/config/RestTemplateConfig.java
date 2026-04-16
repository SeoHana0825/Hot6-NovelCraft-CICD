package com.example.hot6novelcraft.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // XML 응답을 String으로 받기 위한 컨버터 추가
        restTemplate.getMessageConverters().add(
                new StringHttpMessageConverter(StandardCharsets.UTF_8)
        );

        return restTemplate;
    }
}