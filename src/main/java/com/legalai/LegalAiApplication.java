package com.legalai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class LegalAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LegalAiApplication.class, args);
	}

}
