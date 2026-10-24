package com.mycompany.clinica.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.mycompany.clinica.entity.Historial;
import com.mycompany.clinica.repository.HistorialRepository;

@Service
public class HistorialService {

    private final HistorialRepository historialRepository;

    public HistorialService(HistorialRepository historialRepository) {
        this.historialRepository = historialRepository;
    }

    // Listado general. Utilizar solamente desde rutas autorizadas.
    public List<Historial> listarTodos() {
        return historialRepository.findAll();
    }

    public Page<Historial> listarTodos(Pageable pageable) {
        return historialRepository.findAll(pageable);
    }

    public Optional<Historial> buscarPorId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return historialRepository.findById(id);
    }

    public Historial guardar(Historial historial) {
        if (historial == null) {
            throw new IllegalArgumentException("El historial no puede ser nulo.");
        }
        return historialRepository.save(historial);
    }

    public Historial actualizar(Historial historial) {
        if (historial == null || historial.getId() == null || historial.getId().isBlank()) {
            throw new IllegalArgumentException("El historial debe tener un ID para actualizarse.");
        }
        if (!historialRepository.existsById(historial.getId())) {
            throw new IllegalArgumentException("No existe el historial que se intenta actualizar.");
        }
        return historialRepository.save(historial);
    }

    // MÃ©todo heredado: restringir su uso desde controladores.
    // Para historiales clÃ­nicos, preferir anulaciÃ³n lÃ³gica con auditorÃ­a.
    public void eliminar(String id) {
        historialRepository.deleteById(id);
    }

    public boolean existePorId(String id) {
        return id != null && !id.isBlank() && historialRepository.existsById(id);
    }

    public List<Historial> buscarPorUsuario(String usuarioUsername) {
        return historialRepository.findByUsuarioUsername(usuarioUsername);
    }

    public Page<Historial> buscarPorUsuario(String usuarioUsername, Pageable pageable) {
        return historialRepository.findByUsuarioUsername(usuarioUsername, pageable);
    }

    public List<Historial> buscarPorMedico(String medicoUsername) {
        return historialRepository.findByMedicoUsername(medicoUsername);
    }

    // SoluciÃ³n compatible con el repositorio existente.
    // Para colecciones grandes, sustituir por paginaciÃ³n nativa en MongoDB.
    public Page<Historial> buscarPorMedico(String medicoUsername, Pageable pageable) {
        List<Historial> historiales = historialRepository.findByMedicoUsername(medicoUsername);
        int inicio = (int) Math.min(pageable.getOffset(), historiales.size());
        int fin = Math.min(inicio + pageable.getPageSize(), historiales.size());
        return new PageImpl<>(historiales.subList(inicio, fin), pageable, historiales.size());
    }
}

