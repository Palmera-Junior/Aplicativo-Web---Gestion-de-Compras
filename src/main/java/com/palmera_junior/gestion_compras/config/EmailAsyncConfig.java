package com.palmera_junior.gestion_compras.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuración del pool de hilos asíncronos para el envío no bloqueante de correos electrónicos.
 */
@Configuration
public class EmailAsyncConfig {

    /**
     * Qué hace:
     * Configura e inicializa el ejecutor de tareas `emailExecutor` con un tamaño base de 4 hilos,
     * máximo 8 hilos y cola de hasta 50 tareas para procesar la mensajería en segundo plano.
     * 
     * @return {@link Executor} configurado para `@Async("emailExecutor")`.
     */
    @Bean("emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}

