package com.mycompany.clinica.repository;

import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    Optional<Usuario> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<Usuario> findByRol(Rol rol);

    boolean existsByUsernameAndRol(String username, Rol rol);

    Optional<Usuario> findByUsernameAndRol(String username, Rol rol);

    long countByRol(Rol rol);
}