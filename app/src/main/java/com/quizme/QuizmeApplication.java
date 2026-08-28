package com.quizme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient // for service discovery
@EnableAsync
@EnableScheduling // for outbox relay
public class QuizmeApplication {

    static void main(String[] args) {
		SpringApplication.run(QuizmeApplication.class, args);
	}

}
