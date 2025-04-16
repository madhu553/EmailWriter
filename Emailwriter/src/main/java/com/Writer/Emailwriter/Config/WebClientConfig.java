package com.Writer.Emailwriter.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;



@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://generativelanguage.googleapis.com")  // Base URL
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}

