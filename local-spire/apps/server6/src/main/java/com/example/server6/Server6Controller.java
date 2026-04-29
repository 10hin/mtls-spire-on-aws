package com.example.server6;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class Server6Controller {
    @GetMapping("/hello")
    public Mono<String> hello() {
        return Mono.just("Hello, World! (from server6)");
    }
}
