package com.example.workerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class WorkerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkerServiceApplication.class, args);
	}

}
