package com.mycompany.clinica.repository;

import com.mycompany.clinica.entity.Medicamento;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicamentoRepository extends MongoRepository<Medicamento, String> {

    Optional<Medicamento> findByNombre(String nombre);
    Optional<Medicamento> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);

    long countByStockGreaterThan(int stock);
    long countByStockGreaterThan(Integer stock);

    List<Medicamento> findByNombreContainingIgnoreCase(String nombre);
    List<Medicamento> findByEstado(String estado);
    List<Medicamento> findByPrincipioActivoContainingIgnoreCase(String principioActivo);
}
