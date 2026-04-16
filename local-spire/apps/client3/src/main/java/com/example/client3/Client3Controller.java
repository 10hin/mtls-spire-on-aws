package com.example.client3;

import org.objectweb.asm.tree.ModuleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
public class Client3Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client3Controller.class);

    private final WebClient backendClient;

    public Client3Controller(
            final WebClient backendClient
    ) {

        this.backendClient = backendClient;

    }

    @GetMapping("/hello")
    public Mono<String> hello() {
        return Mono.just("Hello, World! (from client3)");
    }
    @PostMapping("/backend")
    public Mono<String> callBackend() {
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
