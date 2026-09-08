package com.mycompany.clinica.repository;

import com.mycompany.clinica.entity.Medicamento;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MedicamentoRepository extends MongoRepository<Medicamento, String> {

    // valida que no existan duplicados por nombre
    Optional<Medicamento> findByNombre(String nombre);
}