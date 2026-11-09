
package com.mycompany.clinica.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.mycompany.clinica.entity.Historial;

public interface HistorialRepository
        extends MongoRepository<Historial, String> {

    // =========================================
    // HISTORIALES DE UN USUARIO
    // =========================================
    List<Historial> findByUsuarioUsername(
            String usuarioUsername
    );

    // =========================================
    // HISTORIALES DE UN USUARIO PAGINADOS
    // =========================================
    Page<Historial> findByUsuarioUsername(
            String usuarioUsername,
            Pageable pageable
    );

    // Consultas por ID real del paciente: evita asociar mediante nombres o documentos.
    List<Historial> findByPacienteId(String pacienteId);
    Page<Historial> findByPacienteId(String pacienteId, Pageable pageable);

    // =========================================
    // HISTORIALES DE UN MÃ‰DICO
    // =========================================
    List<Historial> findByMedicoUsername(
            String medicoUsername
    );

    // =========================================
    // HISTORIALES DE UN MÃ‰DICO PAGINADOS
    // =========================================
    Page<Historial> findByMedicoUsername(
            String medicoUsername,
            Pageable pageable
    );
}

