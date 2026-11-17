package com.mycompany.clinica.repository;

import com.mycompany.clinica.entity.Paciente;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PacienteRepository extends MongoRepository<Paciente, String> {

    @Query("{ 'documento': ?0 }")
    Optional<Paciente> findByDocumento(String documento);

    @Query(value = "{ 'documento': ?0 }", exists = true)
    boolean existsByDocumento(String documento);

    List<Paciente> findByActivoTrue();
    long countByActivoTrue();
    List<Paciente> findByActivoFalse();
    long countByActivoFalse();
}