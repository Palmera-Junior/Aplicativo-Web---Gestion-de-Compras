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
        // 1. Evitamos duplicar datos si ya existen en la base de datos
        if (SedeRepository.count() == 0) {
            
            // Creamos las sedes iniciales
            Sede Bucaramanga = new Sede(null, "Sede Bucaramanga", "BUC", "Calle 36 # 12-34");
            Sede medellin = new Sede(null, "Sede Medellín", "MED", "Carrera 43A # 1-50");
            
            SedeRepository.save(Bucaramanga);
            SedeRepository.save(medellin);

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
            }
        }
    }
}
