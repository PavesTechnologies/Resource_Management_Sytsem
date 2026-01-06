package com.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client")
@CrossOrigin
//@SecurityRequirement(name = "bearerAuth")
public class ClientController {
    @GetMapping("/test")
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'GENERAL')")
    public String test(){
         return "All Done ranga";
    }

    @GetMapping("/testing")
    @PreAuthorize("hasRole('AK')")
    public String testw()
    {
        return "nenu";
    }
}
