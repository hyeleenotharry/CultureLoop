package com.example.CultureLoop.controller;

import org.springframework.web.bind.annotation.GetMapping;

public class HomeController {
    @GetMapping("/")
    public String healthCheck() {
        return "OK";
    }
}
