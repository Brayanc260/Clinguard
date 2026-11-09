package com.mycompany.clinica.controller;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conversiones")
public class ConversionRegistroController {

    private static final Set<String> ROLES =
            Set.of("ROLE_ADMIN", "ROLE_MEDICO", "ROLE_ENFERMERO");

    private final MongoTemplate mongoTemplate;

    public ConversionRegistroController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostMapping("/registros")
    public ResponseEntity<?> guardar(@RequestBody Map<String, Object> cuerpo,
                                     Authentication autenticacion) {
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken
                || autenticacion.getAuthorities().stream()
                    .noneMatch(a -> ROLES.contains(a.getAuthority()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No autorizado."));
        }

        try {
            if (cuerpo == null) {
                throw new IllegalArgumentException("Debes enviar los datos de la conversión.");
            }

            String pacienteId = cadena(cuerpo.get("pacienteId"));
            String operacion = cadena(cuerpo.get("operacion"));
            if (pacienteId.isBlank()) {
                throw new IllegalArgumentException("Selecciona un paciente registrado.");
            }

            Document paciente = buscarPaciente(pacienteId);
            if (paciente == null) {
                throw new IllegalArgumentException("El paciente no existe en MongoDB.");
            }

            double valor = requerido(cuerpo.get("valor"), "Valor");
            Double disponible = opcional(cuerpo.get("dosisDisponible"));
            Double volumen = opcional(cuerpo.get("volumenDisponible"));

            // El peso del paciente se toma de MongoDB, no del dato enviado por el navegador.
            Double peso = opcional(paciente.get("peso"));
            double resultado;
            String unidad;

            switch (operacion) {
                case "g-mg" -> { resultado = valor * 1000; unidad = "mg"; }
                case "mg-g" -> { resultado = valor / 1000; unidad = "g"; }
                case "ug-mg" -> { resultado = valor / 1000; unidad = "mg"; }
                case "kg-g" -> { resultado = valor * 1000; unidad = "g"; }
                case "g-kg" -> { resultado = valor / 1000; unidad = "kg"; }
                case "mg-kg" -> { resultado = valor / 1_000_000; unidad = "kg"; }
                case "ml-l" -> { resultado = valor / 1000; unidad = "L"; }
                case "l-ml" -> { resultado = valor * 1000; unidad = "mL"; }
                case "dosis-mg-kg" -> {
                    resultado = valor / positivo(peso, "Peso del paciente"); unidad = "mg/kg";
                }
                case "dosis-ug-kg" -> {
                    resultado = valor / positivo(peso, "Peso del paciente"); unidad = "µg/kg";
                }
                case "dosis-total" -> {
                    resultado = valor * positivo(peso, "Peso del paciente"); unidad = "mg";
                }
                case "dosis-volumen" -> {
                    resultado = (valor / positivo(disponible, "Dosis disponible"))
                            * positivo(volumen, "Volumen disponible");
                    unidad = "mL";
                }
                default -> throw new IllegalArgumentException("Operación desconocida.");
            }

            if (!Double.isFinite(resultado)) {
                throw new IllegalArgumentException("El cálculo produjo un resultado no válido.");
            }

            Document registro = new Document("pacienteId", pacienteId)
                    .append("pacienteNombre", (cadena(paciente.get("nombres")) + " "
                            + cadena(paciente.get("apellidos"))).trim())
                    .append("pacienteDocumento", cadena(paciente.get("documento")))
                    .append("usuarioUsername", autenticacion.getName())
                    .append("operacion", operacion)
                    .append("valor", valor)
                    .append("pesoPaciente", peso)
                    .append("dosisDisponible", disponible)
                    .append("volumenDisponible", volumen)
                    .append("resultado", resultado)
                    .append("unidad", unidad)
                    .append("fecha", Date.from(Instant.now()));

            mongoTemplate.insert(registro, "conversiones_medicas");
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("mensaje", "Conversión guardada correctamente.");
            respuesta.put("id", String.valueOf(registro.get("_id")));
            respuesta.put("resultado", resultado);
            respuesta.put("unidad", unidad);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private Document buscarPaciente(String id) {
        // MongoDB puede almacenar el identificador como ObjectId o String.
        if (ObjectId.isValid(id)) {
            Document porObjectId = mongoTemplate.findOne(
                    Query.query(Criteria.where("_id").is(new ObjectId(id))),
                    Document.class, "pacientes");
            if (porObjectId != null) return porObjectId;
        }
        return mongoTemplate.findOne(
                Query.query(Criteria.where("_id").is(id)),
                Document.class, "pacientes");
    }

    private String cadena(Object valor) {
        return valor == null ? "" : String.valueOf(valor).trim();
    }

    private Double opcional(Object valor) {
        if (valor == null || cadena(valor).isBlank()) return null;
        final double numero;
        try {
            if (valor instanceof Decimal128 decimal) {
                numero = decimal.bigDecimalValue().doubleValue();
            } else if (valor instanceof Number n) {
                numero = n.doubleValue();
            } else if (valor instanceof String s) {
                numero = Double.parseDouble(s.trim());
            } else {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Los valores deben ser numéricos.");
        }
        if (!Double.isFinite(numero) || numero < 0) {
            throw new IllegalArgumentException("Se requieren valores numéricos válidos y no negativos.");
        }
        return numero;
    }

    private double requerido(Object valor, String nombre) {
        Double numero = opcional(valor);
        if (numero == null) throw new IllegalArgumentException("Falta el campo: " + nombre);
        return numero;
    }

    private double positivo(Double valor, String nombre) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException(nombre + " debe ser mayor que cero.");
        }
        return valor;
    }
}
