package com.mycompany.clinica.repository;

import com.mycompany.clinica.entity.Paciente;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PacienteRepository extends MongoRepository<Paciente, String> {

    Optional<Paciente> findByIdentificacion(String identificacion);
    boolean existsByIdentificacion(String identificacion);

    boolean existsByDocumento(String documento);
    Optional<Paciente> findByDocumento(String documento);

    List<Paciente> findByActivoTrue();
    long countByActivoTrue();

    List<Paciente> findByActivoFalse();
    long countByActivoFalse();
}
