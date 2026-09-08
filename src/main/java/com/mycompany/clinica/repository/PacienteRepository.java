package com.mycompany.clinica.repository;

import com.mycompany.clinica.entity.Paciente;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface PacienteRepository extends MongoRepository<Paciente, String> {
    // busca pacientes por su número de cédula o identificación
    Optional<Paciente> findByIdentificacion(String identificacion);
}