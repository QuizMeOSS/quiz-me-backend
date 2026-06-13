package com.quizme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient // for service discovery
public class QuizmeApplication {

    static void main(String[] args) {
		SpringApplication.run(QuizmeApplication.class, args);
	}

}
