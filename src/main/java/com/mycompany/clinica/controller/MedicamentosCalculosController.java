package com.mycompany.clinica.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/medicamentos")
public class MedicamentosCalculosController {

    private final MongoTemplate mongoTemplate;

    public MedicamentosCalculosController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostMapping("/calculos")
    public ResponseEntity<Map<String, Object>> guardar(
            @RequestBody Map<String, Object> entrada,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_MEDICO") ||
                        a.getAuthority().equals("ROLE_ENFERMERO"))) {
            return fallo(HttpStatus.FORBIDDEN, "No tienes permiso para registrar cÃ¡lculos.");
        }
        try {
            String tipo = String.valueOf(entrada.getOrDefault("tipo", ""));
            if (!tipo.equals("dosis") && !tipo.equals("volumen")) {
                return fallo(HttpStatus.BAD_REQUEST, "Tipo de cÃ¡lculo no vÃ¡lido.");
            }

            Document registro = new Document("tipo", tipo)
                    .append("usuario", authentication.getName())
                    .append("fechaRegistro", java.util.Date.from(Instant.now()));

            Double resultado;
            String unidad;
            String medicamentoId = entrada.get("medicamentoId") == null
                    ? ""
                    : String.valueOf(entrada.get("medicamentoId")).trim();

            Document medicamento = null;
            if (!medicamentoId.isBlank()) {
                Object identificador = ObjectId.isValid(medicamentoId)
                        ? new ObjectId(medicamentoId)
                        : medicamentoId;
                medicamento = mongoTemplate.findOne(
                        Query.query(Criteria.where("_id").is(identificador)),
                        Document.class, "medicamentos");
                if (medicamento == null) {
                    return fallo(HttpStatus.BAD_REQUEST, "El medicamento seleccionado no existe.");
                }
                registro.append("medicamentoId", medicamentoId)
                        .append("medicamento", medicamento.getString("nombre"));
            }

            if (tipo.equals("dosis")) {
                double peso = numero(entrada.get("peso"), "peso");
                double dosisPorKg = numero(entrada.get("dosisIndicada"), "dosis indicada");
                resultado = peso * dosisPorKg;
                unidad = "mg";
                registro.append("pesoKg", peso)
                        .append("dosisIndicadaMgKg", dosisPorKg);

                if (medicamento != null) {
                    String unidadMed = String.valueOf(medicamento.getOrDefault("unidadMedicamento", ""));
                    String unidadVeh = String.valueOf(medicamento.getOrDefault("unidadVehiculo", ""));
                    Object cantidad = medicamento.get("cantidadMedicamento");
                    Object vehiculo = medicamento.get("cantidadVehiculo");
                    if ("mg".equalsIgnoreCase(unidadMed) && "mL".equalsIgnoreCase(unidadVeh)
                            && cantidad instanceof Number c && vehiculo instanceof Number v
                            && c.doubleValue() > 0 && v.doubleValue() > 0) {
                        double volumen = resultado * v.doubleValue() / c.doubleValue();
                        if (!Double.isFinite(volumen))
                            throw new IllegalArgumentException("Resultado fuera de rango.");
                        registro.append("volumenCalculadoMl", volumen);
                    }
                }
            } else {
                double deseada = numero(entrada.get("dosisDeseada"), "dosis deseada");
                double disponible = numero(entrada.get("dosisDisponible"), "dosis disponible");
                double volumen = numero(entrada.get("volumenDisponible"), "volumen disponible");
                resultado = deseada * volumen / disponible;
                unidad = "mL";
                registro.append("dosisDeseadaMg", deseada)
                        .append("dosisDisponibleMg", disponible)
                        .append("volumenDisponibleMl", volumen);
            }
            if (!Double.isFinite(resultado) || resultado <= 0) {
                return fallo(HttpStatus.BAD_REQUEST, "Resultado fuera de rango.");
            }
            registro.append("resultado", resultado).append("unidad", unidad);
            mongoTemplate.insert(registro, "calculos_medicamentos");

            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("guardado", true);
            respuesta.put("resultado", resultado);
            respuesta.put("unidad", unidad);
            if (registro.containsKey("volumenCalculadoMl")) {
                respuesta.put("volumenCalculadoMl", registro.get("volumenCalculadoMl"));
            }
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException ex) {
            return fallo(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private double numero(Object valor, String campo) {
        double n;
        try {
            n = Double.parseDouble(String.valueOf(valor));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Ingresa un valor vÃ¡lido para " + campo + ".");
        }
        if (!Double.isFinite(n) || n <= 0 || n > 1_000_000_000) {
            throw new IllegalArgumentException("El valor de " + campo + " debe ser positivo y vÃ¡lido.");
        }
        return n;
    }

    private ResponseEntity<Map<String, Object>> fallo(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("guardado", false, "error", mensaje));
    }
}

