package com.aitutor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgentTutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentTutorApplication.class, args);
    }
}
