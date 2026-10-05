package com.mycompany.clinica;

import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;
import com.mycompany.clinica.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class ClinicaApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClinicaApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (usuarioRepository.count() == 0) {
                Usuario admin = new Usuario();
                admin.setNombreUsuario("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRol(Rol.ADMIN);
                usuarioRepository.save(admin);

                Usuario medico = new Usuario();
                medico.setNombreUsuario("medico");
                medico.setPassword(passwordEncoder.encode("medico123"));
                medico.setRol(Rol.MEDICO);
                usuarioRepository.save(medico);

                Usuario enfermero = new Usuario();
                enfermero.setNombreUsuario("enfermero");
                enfermero.setPassword(passwordEncoder.encode("enfermero123"));
                enfermero.setRol(Rol.ENFERMERO);
                usuarioRepository.save(enfermero);
                
                System.out.println("Usuarios por defecto creados en MongoDB (admin, medico, enfermero).");
            }
        };
    }
}
