package com.example.ChatIntegrationZoya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class ChatIntegrationZoyaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatIntegrationZoyaApplication.class, args);
	}

}
