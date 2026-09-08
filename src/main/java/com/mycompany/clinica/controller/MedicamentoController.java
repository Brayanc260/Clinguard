package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Medicamento;
import com.mycompany.clinica.repository.MedicamentoRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicamentos")
@CrossOrigin(origins = "*")
public class MedicamentoController {

    private final MedicamentoRepository medRepo;

    public MedicamentoController(MedicamentoRepository medRepo) {
        this.medRepo = medRepo;
    }

    @GetMapping
    public ResponseEntity<List<Medicamento>> getAll() {
        return ResponseEntity.ok(medRepo.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medicamento> getById(@PathVariable String id) {
        return medRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<?> crear(@Valid @RequestBody Medicamento medicamento) {
        if (medRepo.findByNombre(medicamento.getNombre()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Ya existe un medicamento con el nombre " + medicamento.getNombre());
        }
        return ResponseEntity.ok(medRepo.save(medicamento));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<?> editar(@PathVariable String id, @Valid @RequestBody Medicamento datos) {
        return medRepo.findById(id).map(m -> {
            m.setNombre(datos.getNombre());
            m.setDosisMinimaMgKg(datos.getDosisMinimaMgKg());
            m.setDosisMaximaMgKg(datos.getDosisMaximaMgKg());
            m.setDescripcion(datos.getDescripcion());
            return ResponseEntity.ok(medRepo.save(m));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminar(@PathVariable String id) {
        if (!medRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        medRepo.deleteById(id);
        return ResponseEntity.ok("Medicamento eliminado correctamente.");
    }
}