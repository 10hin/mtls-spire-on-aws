package com.example.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BackendMainController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, this is backend";
    }
}
