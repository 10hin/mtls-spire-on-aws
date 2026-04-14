package com.example.server2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Server2Controller {
    @GetMapping("/hello")
    public String hello() {
        return "Hello, World! (from server2)";
    }
}
