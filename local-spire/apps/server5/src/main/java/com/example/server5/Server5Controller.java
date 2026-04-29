package com.example.server5;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Server5Controller {
    @GetMapping("/hello")
    public String hello() {
        return "Hello, World! (from server5)";
    }
}
