package com.palmera_junior.gestion_compras.config;

import com.palmera_junior.gestion_compras.entity.*;
import com.palmera_junior.gestion_compras.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SedeRepository SedeRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SedeRepository SedeRepository, 
                           UsuarioRepository usuarioRepository, 
                           PasswordEncoder passwordEncoder) {
        this.SedeRepository = SedeRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println("====== INICIANDO COMPROBACIÓN DE SEMILLA DE DATOS ======");
    System.out.println("Cantidad de usuarios actuales: " + usuarioRepository.count());
    
    if (usuarioRepository.count() == 0) {
        System.out.println("Creando datos de prueba...");
        
        // ... (Tu código de creación de Sede y Usuarios) ...
        
        System.out.println("¡USUARIOS CREADOS EXITOSAMENTE!");
        System.out.println("Prueba loguearte con: admin / admin123");
    } else {
        System.out.println("La base de datos ya tiene usuarios. SE OMITIÓ la creación de la semilla.");
    }
    System.out.println("========================================================");
        // 1. Evitamos duplicar datos si ya existen en la base de datos
        if (SedeRepository.count() == 0) {
            
            // Creamos las sedes iniciales
            Sede Bucaramanga = new Sede(null, "Sede Bucaramanga", "BUC", "Calle 36 # 12-34");
            Sede medellin = new Sede(null, "Sede Medellín", "MED", "Carrera 43A # 1-50");
            Sede Nacional = new Sede(null, "Sede Nacional", "NAC", "Calle 100 # 20-30");

            SedeRepository.save(Bucaramanga);
            SedeRepository.save(medellin);
            SedeRepository.save(Nacional);

            if (usuarioRepository.count() == 0) {
                // 2. Creamos un Administrador Global (ve todo)
                Usuario admin = new Usuario();
                admin.setCedula("1098765432");
                admin.setNombre("Felipe");
                admin.setApellido("Corzo");
                admin.setCargo("Director de Backend");
                admin.setNombreUsuario("admin");
                // ENCRIPTACIÓN CRÍTICA: Nunca guardes texto plano
                admin.setContraseña(passwordEncoder.encode("admin123")); 
                admin.setRol(Rol.ADMINISTRADOR);
                admin.setSede(Bucaramanga);
                usuarioRepository.save(admin);

                // 3. Creamos un Auxiliar de Sede (solo ve datos de Bucaramanga)
                Usuario auxiliarBuc = new Usuario();
                auxiliarBuc.setCedula("1234567890");
                auxiliarBuc.setNombre("Juan");
                auxiliarBuc.setApellido("Pérez");
                auxiliarBuc.setCargo("Auxiliar de Compras");
                auxiliarBuc.setNombreUsuario("auxbuc");
                auxiliarBuc.setContraseña(passwordEncoder.encode("auxbuc123"));
                auxiliarBuc.setRol(Rol.USUARIO);
                auxiliarBuc.setSede(Bucaramanga);
                usuarioRepository.save(auxiliarBuc);

                Usuario auxiliarMed = new Usuario();
                auxiliarMed.setCedula("0987654321");
                auxiliarMed.setNombre("Andrey");
                auxiliarMed.setApellido("Gómez");
                auxiliarMed.setCargo("Auxiliar de IT");
                auxiliarMed.setNombreUsuario("auxmed");
                auxiliarMed.setContraseña(passwordEncoder.encode("auxmed123"));
                auxiliarMed.setRol(Rol.USUARIO);
                auxiliarMed.setSede(medellin);
                usuarioRepository.save(auxiliarMed);

                Usuario auxiliarNac    = new Usuario();
                auxiliarNac.setCedula("1122334455");
                auxiliarNac.setNombre("Carlos");  
                auxiliarNac.setApellido("Ramírez");
                auxiliarNac.setCargo("Auxiliar de Logística");
                auxiliarNac.setNombreUsuario("auxnac"); 
                auxiliarNac.setContraseña(passwordEncoder.encode("auxnac123"));
                auxiliarNac.setRol(Rol.USUARIO);
                auxiliarNac.setSede(Nacional); // O asigna una sede específica si es necesario
                usuarioRepository.save(auxiliarNac);
            }
        }


    }
}
//la insercion de datos que se hace en DataInitializer si es el orden y nombres de campos correctos con la base de datos? por que al ejecutar no se guarda nada en la base de datos