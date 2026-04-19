package com.example.client4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
public class Client4Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client4Controller.class);

    private final WebClient backendClient;

    public Client4Controller(
            final WebClient backendClient
    ) {

        this.backendClient = backendClient;

    }

    @GetMapping("/hello")
    public Mono<String> hello() {
        return Mono.just("Hello, World! (from client4)");
    }

    @PostMapping("/backend")
    public Mono<String> callBackground() {
        return this.backendClient.get()
                .uri("/hello")
                .retrieve()
                .toEntity(String.class)
                .doOnNext((backendResp) -> {
                    LOGGER.info("backend response status: {}", backendResp.getStatusCode().value());
                    LOGGER.info("backend response body: {}", backendResp.getBody());
                })
                .map(backendResp -> "result of calling backend: " + backendResp.getBody());
    }

}
