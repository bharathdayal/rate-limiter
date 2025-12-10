package com.example.ratelimit.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, world!";
    }

    @GetMapping("/api/fast")
    public String fastEndpoint() {
        return "Fast endpoint: higher rate";
    }

    @GetMapping("/api/slow")
    public String slowEndpoint() {
        return "Slow endpoint: lower rate";
    }

}
