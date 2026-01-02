package br.com.rentafit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class RentafitApplication {

	public static void main(String[] args) {
		SpringApplication.run(RentafitApplication.class, args);
	}

}
