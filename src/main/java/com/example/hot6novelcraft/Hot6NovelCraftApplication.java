package com.example.hot6novelcraft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class Hot6NovelCraftApplication {

    public static void main(String[] args) {
        SpringApplication.run(Hot6NovelCraftApplication.class, args);
    }

}
