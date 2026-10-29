
package com.mycompany.clinica.repository;

import com.mycompany.clinica.entity.Paciente;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PacienteRepository
        extends MongoRepository<Paciente, String> {

    // BUSCAR PACIENTE POR DOCUMENTO
    Optional<Paciente> findByDocumento(String documento);

    // VERIFICAR SI EXISTE UN DOCUMENTO
    boolean existsByDocumento(String documento);

    // PACIENTES ACTIVOS
    List<Paciente> findByActivoTrue();
    long countByActivoTrue();

    // PACIENTES INACTIVOS
    List<Paciente> findByActivoFalse();
    long countByActivoFalse();
}
