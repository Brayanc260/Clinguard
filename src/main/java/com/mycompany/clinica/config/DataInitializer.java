package com.mycompany.clinica.config;

import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;
import com.mycompany.clinica.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsuarios(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            try {
                crearOActualizar(usuarioRepository, passwordEncoder, "admin", "Administrador", "admin@clinguard.com", "3000000000", "Admin123", Rol.ADMIN);
                crearOActualizar(usuarioRepository, passwordEncoder, "medico", "Médico", "medico@clinguard.com", "3001111111", "Medico123", Rol.MEDICO);
                crearOActualizar(usuarioRepository, passwordEncoder, "enfermero", "Enfermero", "enfermero@clinguard.com", "3002222222", "Enfermero123", Rol.ENFERMERO);
                crearOActualizar(usuarioRepository, passwordEncoder, "usuario", "Usuario", "usuario@clinguard.com", "3003333333", "Usuario123", Rol.USUARIO);
            } catch (Exception e) {
                System.err.println("CLINGUARD: No se pudo conectar con MongoDB. Error: " + e.getMessage());
            }
        };
    }

    private void crearOActualizar(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            String username, String nombre, String email, String telefono, String password, Rol rol) {

        Usuario usuario = usuarioRepository.findByNombreUsuario(username).orElse(null);

        if (usuario == null) {
            usuario = new Usuario();
            usuario.setNombreUsuario(username);
            usuario.setPassword(passwordEncoder.encode(password));
        }
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setTelefono(telefono);
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }
}