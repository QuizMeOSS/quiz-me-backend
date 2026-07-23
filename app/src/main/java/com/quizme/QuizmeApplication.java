package com.quizme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient // for service discovery
@EnableAsync
public class QuizmeApplication {

    static void main(String[] args) {
		SpringApplication.run(QuizmeApplication.class, args);
	}

}
