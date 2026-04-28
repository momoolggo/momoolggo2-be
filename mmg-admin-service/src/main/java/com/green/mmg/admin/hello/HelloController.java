package com.green.mmg.admin.hello;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from admin-service (port 8083)";
    }
}
