package com.personaflow.commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PersonaCommerceServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonaCommerceServerApplication.class, args);
	}

}
