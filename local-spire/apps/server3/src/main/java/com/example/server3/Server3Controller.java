package com.example.server3;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class Server3Controller {
    @GetMapping("/hello")
    public Mono<String> hello() {
        return Mono.just("Hello, World! (from server3)");
    }
}
