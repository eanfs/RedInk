package com.redink.config;

import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * AI配置类
 * 简化为只支持 OpenAI 图像生成功能
 */
@Configuration
public class AiConfig {

    /**
     * OpenAI图片模型 (用于图像生成)
     * 使用 Spring Boot 自动配置，无需手动配置
     */
    @Bean
    @Primary
    public OpenAiImageModel openAiImageModel(OpenAiImageApi openAiImageApi) {
        return new OpenAiImageModel(openAiImageApi);
    }

    /**
     * 通用HTTP客户端
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestFactory(null)
                .build();
    }
}