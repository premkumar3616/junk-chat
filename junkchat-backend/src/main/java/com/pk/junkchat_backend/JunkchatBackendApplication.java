package com.pk.junkchat_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class JunkchatBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JunkchatBackendApplication.class, args);
	}

}
