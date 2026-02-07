package com.kd.signOn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.kd.signOn.utils.TlsBootstrap;

@SpringBootApplication
public class SignOnApplication {

	public static void main(String[] args) {
		try {
			TlsBootstrap.initialize();
		} catch (Exception e) {
			System.err.println("Failed to initialize TLS: ");
			System.exit(1);
		}
		SpringApplication.run(SignOnApplication.class, args);
	}

}
