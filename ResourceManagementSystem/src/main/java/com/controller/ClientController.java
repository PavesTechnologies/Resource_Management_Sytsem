package com.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/client")
@EnableMethodSecurity
@CrossOrigin
public class ClientController {
    @GetMapping("/test")
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'GENERAL')")
    public void test(){
        System.out.println("All Done");
    }
}
