package com.palmera_junior.gestion_compras;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class GestionComprasApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionComprasApplication.class, args);
	}

}
