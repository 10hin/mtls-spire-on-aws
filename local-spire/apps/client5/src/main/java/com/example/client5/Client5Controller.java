package com.example.client5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class Client5Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client5Controller.class);

    private final RestClient backendClient;

    public Client5Controller(
            final RestClient backendClient
    ) {

        this.backendClient = backendClient;

    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World! (from client5)";
    }

    @PostMapping("/backend")
    public String callBackend() {
        final var backendResp = this.backendClient.get()
                .uri("/hello")
                .retrieve()
                .toEntity(String.class);
        LOGGER.info("backend response status: {}", backendResp.getStatusCode().value());
        LOGGER.info("backend response body: {}", backendResp.getBody());
        return "result of calling backend: " + backendResp.getBody();
    }

}
