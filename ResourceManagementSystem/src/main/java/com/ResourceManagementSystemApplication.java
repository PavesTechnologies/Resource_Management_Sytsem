package com;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class 	ResourceManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourceManagementSystemApplication.class, args);
	}

}
