package com.story.relay.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${ai-servers.analysis.url}")
    private String analysisAiUrl;

    @Value("${ai-servers.analysis.timeout}")
    private int analysisTimeout;

    @Value("${ai-servers.image-generation.url}")
    private String imageGenerationAiUrl;

    @Value("${ai-servers.image-generation.timeout}")
    private int imageGenerationTimeout;

    @Value("${ai-servers.rag.url:http://localhost:8002}")
    private String ragServerUrl;

    @Value("${ai-servers.rag.timeout:10000}")
    private int ragTimeout;

    @Bean
    public WebClient analysisAiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, analysisTimeout)
                .responseTimeout(Duration.ofMillis(analysisTimeout))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(analysisTimeout, TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(analysisTimeout, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(analysisAiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient imageGenerationAiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, imageGenerationTimeout)
                .responseTimeout(Duration.ofMillis(imageGenerationTimeout))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(imageGenerationTimeout, TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(imageGenerationTimeout, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(imageGenerationAiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient ragServerWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ragTimeout)
                .responseTimeout(Duration.ofMillis(ragTimeout))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(ragTimeout, TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(ragTimeout, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(ragServerUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
