package com.green.mmg.rider.hello;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rider")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from rider-service (port 8082)";
    }
}
