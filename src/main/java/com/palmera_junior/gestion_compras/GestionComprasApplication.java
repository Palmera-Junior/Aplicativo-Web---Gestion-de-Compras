package com.palmera_junior.gestion_compras;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal y punto de entrada de la aplicación Spring Boot "Gestión de Compras".
 * Habilita soporte para caché (@EnableCaching), tareas asíncronas (@EnableAsync)
 * y programación periódica (@EnableScheduling).
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class GestionComprasApplication {

    /**
     * Qué hace:
     * Inicializa el contexto de Spring Boot y arranca el servidor web embebido (Tomcat).
     * 
     * @param args Argumentos pasados por línea de comandos.
     */
	public static void main(String[] args) {
		SpringApplication.run(GestionComprasApplication.class, args);
	}

}
