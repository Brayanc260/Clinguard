package com.mycompany.clinica.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conversiones")
public class ConversionRegistroController {
    private final MongoTemplate mongoTemplate;
    private static final Set<String> ROLES = Set.of("ROLE_ADMIN", "ROLE_MEDICO", "ROLE_ENFERMERO");

    public ConversionRegistroController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostMapping("/registros")
    public ResponseEntity<?> guardar(@RequestBody Map<String, Object> cuerpo, Authentication autenticacion) {
        if (autenticacion == null || !autenticacion.isAuthenticated() || autenticacion.getAuthorities().stream()
                .noneMatch(a -> ROLES.contains(a.getAuthority()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "No autorizado."));
        }
        try {
            String pacienteId = cadena(cuerpo.get("pacienteId"));
            String operacion = cadena(cuerpo.get("operacion"));
            if (pacienteId.isBlank())
                throw new IllegalArgumentException("Selecciona un paciente registrado.");
            if (!pacienteId.matches("[a-fA-F0-9]{24}"))
                throw new IllegalArgumentException("Identificador de paciente no vÃ¡lido.");
            Document paciente = mongoTemplate.findById(pacienteId, Document.class, "pacientes");
            if (paciente == null)
                throw new IllegalArgumentException("El paciente no existe en MongoDB.");
            double valor = requerido(cuerpo.get("valor"), "Valor");
            Double peso = opcional(cuerpo.get("pesoPaciente"));
            Double disponible = opcional(cuerpo.get("dosisDisponible"));
            Double volumen = opcional(cuerpo.get("volumenDisponible"));
            double resultado;
            String unidad;
            switch (operacion) {
                case "g-mg" -> {
                    resultado = valor * 1000;
                    unidad = "mg";
                }
                case "mg-g" -> {
                    resultado = valor / 1000;
                    unidad = "g";
                }
                case "ug-mg" -> {
                    resultado = valor / 1000;
                    unidad = "mg";
                }
                case "kg-g" -> {
                    resultado = valor * 1000;
                    unidad = "g";
                }
                case "g-kg" -> {
                    resultado = valor / 1000;
                    unidad = "kg";
                }
                case "mg-kg" -> {
                    resultado = valor / 1_000_000;
                    unidad = "kg";
                }
                case "ml-l" -> {
                    resultado = valor / 1000;
                    unidad = "L";
                }
                case "l-ml" -> {
                    resultado = valor * 1000;
                    unidad = "mL";
                }
                case "dosis-mg-kg" -> {
                    resultado = valor / positivo(peso, "Peso del paciente");
                    unidad = "mg/kg";
                }
                case "dosis-ug-kg" -> {
                    resultado = valor / positivo(peso, "Peso del paciente");
                    unidad = "Âµg/kg";
                }
                case "dosis-total" -> {
                    resultado = valor * positivo(peso, "Peso del paciente");
                    unidad = "mg";
                }
                case "dosis-volumen" -> {
                    resultado = (valor / positivo(disponible, "Dosis disponible"))
                            * positivo(volumen, "Volumen disponible");
                    unidad = "mL";
                }
                default -> throw new IllegalArgumentException("OperaciÃ³n desconocida.");
            }
            if (!Double.isFinite(resultado) || !Double.isFinite(valor) || valor < 0)
                throw new IllegalArgumentException("El cÃ¡lculo contiene valores no vÃ¡lidos.");
            Document d = new Document("pacienteId", pacienteId)
                    .append("pacienteNombre",
                            (cadena(paciente.get("nombres")) + " " + cadena(paciente.get("apellidos"))).trim())
                    .append("pacienteDocumento", cadena(paciente.get("documento")))
                    .append("usuarioUsername", autenticacion.getName())
                    .append("operacion", operacion).append("valor", valor)
                    .append("pesoPaciente", peso).append("dosisDisponible", disponible)
                    .append("volumenDisponible", volumen).append("resultado", resultado)
                    .append("unidad", unidad).append("fecha", java.util.Date.from(Instant.now()));
            mongoTemplate.insert(d, "conversiones_medicas");
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("mensaje", "ConversiÃ³n guardada correctamente.");
            respuesta.put("id", d.get("_id").toString());
            respuesta.put("resultado", resultado);
            respuesta.put("unidad", unidad);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private String cadena(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private Double opcional(Object o) {
        if (o == null || cadena(o).isBlank())
            return null;
        if (!(o instanceof Number n))
            throw new IllegalArgumentException("Los valores deben ser numÃ©ricos.");
        double d = n.doubleValue();
        if (!Double.isFinite(d) || d < 0)
            throw new IllegalArgumentException("Se requieren valores numÃ©ricos vÃ¡lidos y no negativos.");
        return d;
    }

    private double requerido(Object o, String nombre) {
        Double d = opcional(o);
        if (d == null)
            throw new IllegalArgumentException("Falta el campo: " + nombre);
        return d;
    }

    private double positivo(Double d, String nombre) {
        if (d == null || d <= 0)
            throw new IllegalArgumentException(nombre + " debe ser mayor que cero.");
        return d;
    }
}

