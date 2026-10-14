package com.example.ArtConnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // closes finished auctions, releases unpaid reservations and purges expired login tokens
@EnableAsync // invoice emails and texts are sent in the background
public class ArtConnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArtConnectApplication.class, args);
	}

}
