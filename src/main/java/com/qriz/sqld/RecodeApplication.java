package com.qriz.sqld;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class RecodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecodeApplication.class, args);
    }

}