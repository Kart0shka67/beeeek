package com.example.tasks;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SecurityScheme(name = "basic", type = SecuritySchemeType.HTTP, scheme = "basic")
public class TasksApplication {

    static void main(String[] args) {
        SpringApplication.run(TasksApplication.class, args);
    }
}
