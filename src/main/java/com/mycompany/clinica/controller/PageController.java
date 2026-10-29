
package com.mycompany.clinica.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final MongoTemplate mongoTemplate;

    public PageController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // =========================================
    // CONVERSIONES MÃƒÆ’Ã¢â‚¬Â°DICAS
    // =========================================

    @GetMapping("/conversiones")
    public String conversiones(Model model) {

        List<Map<String, String>> opciones = new ArrayList<>();

        Query query = new Query().with(
            Sort.by(
                Sort.Order.asc("apellidos"),
                Sort.Order.asc("nombres")
            )
        );

        List<Document> pacientes = mongoTemplate.find(
            query,
            Document.class,
            "pacientes"
        );

        for (Document p : pacientes) {

            Map<String, String> fila = new LinkedHashMap<>();

            fila.put(
                "id",
                String.valueOf(p.get("_id"))
            );

            fila.put(
                "nombre",
                (String.valueOf(p.getOrDefault("nombres", ""))
                + " "
                + String.valueOf(p.getOrDefault("apellidos", ""))).trim()
            );

            fila.put(
                "documento",
                String.valueOf(p.getOrDefault("documento", ""))
            );

            Object peso = p.get("peso");
            fila.put("pesoKg", peso == null ? "" : String.valueOf(peso));
            opciones.add(fila);
        }

        model.addAttribute("pacientesConversion", opciones);

        return "conversiones";
    }

    // =========================================
    // ANÃƒÆ’Ã‚ÂLISIS
    // =========================================

    @GetMapping("/analisis")
    public String analisis(Model model) {

        model.addAttribute(
            "totalPacientes",
            mongoTemplate.count(new Query(), "pacientes")
        );

        model.addAttribute(
            "totalCitas",
            mongoTemplate.count(new Query(), "citas")
        );

        model.addAttribute(
            "totalHistoriales",
            mongoTemplate.count(new Query(), "historiales")
        );

        model.addAttribute(
            "totalMedicamentos",
            mongoTemplate.count(new Query(), "medicamentos")
        );

        List<Document> citas = mongoTemplate.find(
            new Query(), Document.class, "citas"
        );

        List<Document> historiales = mongoTemplate.find(
            new Query(), Document.class, "historiales"
        );

        List<Document> pacientes = mongoTemplate.find(
            new Query(), Document.class, "pacientes"
        );

        Map<String, Long> porEstado = new LinkedHashMap<>();

        long programadas = 0;
        long confirmadas = 0;
        long atendidas = 0;
        long canceladas = 0;

        for (Document cita : citas) {

            String estado = String.valueOf(
                cita.getOrDefault("estado", "SIN ESTADO")
            ).trim().toUpperCase(Locale.ROOT);

            if (estado.isEmpty()) {
                estado = "SIN ESTADO";
            }

            porEstado.merge(estado, 1L, Long::sum);

            if (estado.contains("PROGRAM")
                    || estado.contains("PENDIENT")) {
                programadas++;
            }

            if (estado.contains("CONFIRM")) {
                confirmadas++;
            }

            if (estado.contains("ATEND")
                    || estado.contains("COMPLET")
                    || estado.contains("FINALIZ")) {
                atendidas++;
            }

            if (estado.contains("CANCEL")) {
                canceladas++;
            }
        }

        model.addAttribute("citasPorEstado", porEstado);
        model.addAttribute("citasProgramadas", programadas);
        model.addAttribute("citasConfirmadas", confirmadas);
        model.addAttribute("citasAtendidas", atendidas);
        model.addAttribute("citasCanceladas", canceladas);

        // =========================================
        // ACTIVIDAD ÃƒÆ’Ã…Â¡LTIMOS 7 DÃƒÆ’Ã‚ÂAS
        // =========================================

        LocalDate hoy = LocalDate.now(
            ZoneId.of("America/Bogota")
        );

        List<String> dias = new ArrayList<>();
        List<Long> citasSemana = new ArrayList<>();
        List<Long> historialesSemana = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {

            LocalDate fecha = hoy.minusDays(i);

            dias.add(fecha.toString());

            citasSemana.add(
                contarEnFecha(citas, "fecha", fecha)
            );

            historialesSemana.add(
                contarEnFecha(historiales, "fecha", fecha)
            );
        }

        model.addAttribute("actividadDias", dias);
        model.addAttribute("actividadCitas", citasSemana);
        model.addAttribute("actividadHistoriales", historialesSemana);

        // =========================================
        // ACTIVIDAD ÃƒÆ’Ã…Â¡LTIMOS 6 MESES
        // =========================================

        List<String> meses = new ArrayList<>();
        List<Long> pacientesPorMes = new ArrayList<>();
        List<Long> historialesPorMes = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {

            YearMonth mes = YearMonth.from(
                hoy.minusMonths(i)
            );

            meses.add(mes.toString());

            pacientesPorMes.add(
                contarEnMes(pacientes, "fechaRegistro", mes)
            );

            historialesPorMes.add(
                contarEnMes(historiales, "fecha", mes)
            );
        }

        model.addAttribute("meses", meses);
        model.addAttribute("pacientesPorMes", pacientesPorMes);
        model.addAttribute("historialesPorMes", historialesPorMes);

        return "analisis";
    }

    // =========================================
    // CONTAR REGISTROS POR FECHA
    // =========================================

    private long contarEnFecha(
            List<Document> documentos,
            String campo,
            LocalDate fecha) {

        return documentos.stream()
            .filter(d -> fecha.equals(
                leerFecha(d.get(campo))
            ))
            .count();
    }

    // =========================================
    // CONTAR REGISTROS POR MES
    // =========================================

    private long contarEnMes(
            List<Document> documentos,
            String campo,
            YearMonth mes) {

        return documentos.stream()
            .map(d -> leerFecha(d.get(campo)))
            .filter(f -> f != null
                && YearMonth.from(f).equals(mes))
            .count();
    }

    // =========================================
    // LEER FECHAS DE MONGODB
    // =========================================

    private LocalDate leerFecha(Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor instanceof Date) {

            return ((Date) valor)
                .toInstant()
                .atZone(ZoneId.of("America/Bogota"))
                .toLocalDate();
        }

        String texto = valor.toString().trim();

        if (texto.length() >= 10) {

            try {
                return LocalDate.parse(
                    texto.substring(0, 10)
                );
            } catch (DateTimeParseException ignorado) {
            }
        }

        try {

            return Instant.parse(texto)
                .atZone(ZoneId.of("America/Bogota"))
                .toLocalDate();

        } catch (DateTimeParseException ignorado) {

            return null;
        }
    }

    // =========================================
    // REPORTES
    // =========================================

    @GetMapping("/reportes")
    public String reportes(Model model) {

        model.addAttribute(
            "totalPacientes",
            mongoTemplate.count(new Query(), "pacientes")
        );

        model.addAttribute(
            "totalCitas",
            mongoTemplate.count(new Query(), "citas")
        );

        model.addAttribute(
            "totalHistoriales",
            mongoTemplate.count(new Query(), "historiales")
        );

        return "reportes";
    }
}

