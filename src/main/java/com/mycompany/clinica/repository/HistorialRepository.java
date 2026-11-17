package com.mycompany.clinica.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import com.mycompany.clinica.entity.Historial;

public interface HistorialRepository extends MongoRepository<Historial, String> {

    @Query("{ 'usuarioUsername': ?0 }")
    List<Historial> findByUsuarioUsername(String usuarioUsername);

    @Query("{ 'usuarioUsername': ?0 }")
    Page<Historial> findByUsuarioUsername(String usuarioUsername, Pageable pageable);

    @Query("{ 'pacienteId': ?0 }")
    List<Historial> findByPacienteId(String pacienteId);

    @Query("{ 'pacienteId': ?0 }")
    Page<Historial> findByPacienteId(String pacienteId, Pageable pageable);

    @Query("{ 'medicoUsername': ?0 }")
    List<Historial> findByMedicoUsername(String medicoUsername);

    @Query("{ 'medicoUsername': ?0 }")
    Page<Historial> findByMedicoUsername(String medicoUsername, Pageable pageable);
}