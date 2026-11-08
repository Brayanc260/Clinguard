package com.mycompany.clinica.controller;

import com.mycompany.clinica.dto.ConversionRequest;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/conversiones")

public class ConversionController {

    private final MongoTemplate mongoTemplate;

    public ConversionController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostMapping("/calcular")
    public ResponseEntity<?> calcularYGuardar(@RequestBody ConversionRequest req, Authentication auth) {
        try {
            // 1. Obtener datos del paciente y medicamento (Manejando ObjectId)
            Document paciente = buscarPorId(req.getPacienteId(), "pacientes");
            Document medicamento = buscarPorId(req.getMedicamentoId(), "medicamentos");

            if (paciente == null || medicamento == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Paciente o medicamento no encontrado."));
            }

            // 2. Calcular la dosis segura
            double peso = Double.parseDouble(String.valueOf(paciente.getOrDefault("peso", 0)));
            double minKg = Double.parseDouble(String.valueOf(medicamento.getOrDefault("dosisMinimaMgKg", 0)));
            double maxKg = Double.parseDouble(String.valueOf(medicamento.getOrDefault("dosisMaximaMgKg", 0)));
            
            double dosisMinTotal = peso * minKg;
            double dosisMaxTotal = peso * maxKg;
            double dosisRecetada = req.getDosisRecetada() != null ? req.getDosisRecetada() : 0;

            // 3. Evaluar el resultado (Semáforo)
            String estado = "SEGURA";
            String mensaje = "La dosis es correcta dentro del rango.";
            
            if (dosisRecetada < dosisMinTotal) {
                estado = "INFRADOSIS";
                mensaje = "La dosis es menor al mínimo recomendado.";
            } else if (dosisRecetada > dosisMaxTotal) {
                estado = "SOBREDOSIS";
                mensaje = "La dosis supera el máximo permitido.";
            }

            // 4. Crear el registro de auditoría
            Document registro = new Document();
            registro.put("usuario", auth != null ? auth.getName() : "Desconocido");
            registro.put("fecha", LocalDateTime.now().toString());
            registro.put("pacienteId", req.getPacienteId());
            registro.put("medicamentoId", req.getMedicamentoId());
            registro.put("dosisRecetada", dosisRecetada);
            registro.put("estado", estado);
            registro.put("mensaje", mensaje);
            registro.put("dosisMinTotal", dosisMinTotal);
            registro.put("dosisMaxTotal", dosisMaxTotal);

            // 5. Guardar en MongoDB automáticamente
            mongoTemplate.save(registro, "historial_conversiones");

            // 6. Devolver respuesta al frontend
            return ResponseEntity.ok(Map.of(
                "estado", estado,
                "mensaje", mensaje,
                "dosisMinTotal", dosisMinTotal,
                "dosisMaxTotal", dosisMaxTotal,
                "usuario", registro.get("usuario"),
                "fecha", registro.get("fecha")
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al procesar: " + e.getMessage()));
        }
    }

    // Método auxiliar para buscar en Mongo soportando ObjectId o String
    private Document buscarPorId(String id, String coleccion) {
        if (id == null || id.isBlank()) return null;
        if (ObjectId.isValid(id)) {
            Document doc = mongoTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(id))), Document.class, coleccion);
            if (doc != null) return doc;
        }
        return mongoTemplate.findOne(new Query(Criteria.where("_id").is(id)), Document.class, coleccion);
    }
}