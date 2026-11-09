package com.mycompany.clinica.repository;

import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    Optional<Usuario> findByEmail(String email);
    boolean existsByNombreUsuario(String nombreUsuario);
    boolean existsByEmail(String email);
    List<Usuario> findByRol(Rol rol);
    long countByRol(Rol rol);

    // =========================================================
    // TRUCO DE COMPATIBILIDAD
    // Esto hace que el código viejo que busca "findByUsername"
    // funcione automáticamente sin dar error.
    // =========================================================
    default Optional<Usuario> findByUsername(String username) {
        return findByNombreUsuario(username);
    }

    default boolean existsByUsername(String username) {
        return existsByNombreUsuario(username);
    }
}