package com.sla.matercard.inbound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@SpringBootApplication
public class TransfastApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransfastApplication.class, args);
    }

    @Bean
    WebClient injectWebClient() {
        HttpClient client = HttpClient.create().responseTimeout(Duration.ofSeconds(60));

        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(client))
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // Buffer size limit increase
                .build();
    }
}
