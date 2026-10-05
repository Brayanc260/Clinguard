package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Paciente;
import com.mycompany.clinica.repository.PacienteRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
@CrossOrigin(origins = "*")
public class PacienteController {

    private final PacienteRepository pacienteRepo;

    public PacienteController(PacienteRepository pacienteRepo) {
        this.pacienteRepo = pacienteRepo;
    }

    @GetMapping
    public ResponseEntity<List<Paciente>> getAll() {
        return ResponseEntity.ok(pacienteRepo.findAll());
    }

    @GetMapping("/paged")
    public ResponseEntity<org.springframework.data.domain.Page<Paciente>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(pacienteRepo.findAll(
            org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("nombre").ascending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Paciente> getById(@PathVariable String id) {
        return pacienteRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    public ResponseEntity<?> crear(@Valid @RequestBody Paciente paciente) {
        if (pacienteRepo.findByIdentificacion(paciente.getIdentificacion()).isPresent()) {
            return ResponseEntity.badRequest().body("Ya existe un paciente con esa identificación.");
        }
        return ResponseEntity.ok(pacienteRepo.save(paciente));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMERO')")
    public ResponseEntity<?> editar(@PathVariable String id, @Valid @RequestBody Paciente datos) {
        return pacienteRepo.findById(id).map(p -> {
            p.setNombre(datos.getNombre());
            p.setIdentificacion(datos.getIdentificacion());
            p.setPesoKg(datos.getPesoKg());
            p.setEdad(datos.getEdad());
            return ResponseEntity.ok(pacienteRepo.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminar(@PathVariable String id) {
        if (!pacienteRepo.existsById(id)) return ResponseEntity.notFound().build();
        pacienteRepo.deleteById(id);
        return ResponseEntity.ok("Paciente eliminado.");
    }
}