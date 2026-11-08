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
import org.springframework.data.mongodb.core.query.Criteria;
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
    // CONVERSIONES MÉDICAS
    // =========================================
    @GetMapping("/conversiones")
    public String conversiones(Model model) {
        List<Map<String, String>> opciones = new ArrayList<>();
        Query query = new Query().with(Sort.by(Sort.Order.asc("apellidos"), Sort.Order.asc("nombres")));
        List<Document> pacientes = mongoTemplate.find(query, Document.class, "pacientes");

        for (Document p : pacientes) {
            Map<String, String> fila = new LinkedHashMap<>();
            fila.put("id", String.valueOf(p.get("_id")));
            fila.put("nombre", (String.valueOf(p.getOrDefault("nombres", "")) + " " + String.valueOf(p.getOrDefault("apellidos", ""))).trim());
            fila.put("documento", String.valueOf(p.getOrDefault("documento", "")));
            Object peso = p.get("peso");
            fila.put("pesoKg", peso == null ? "" : String.valueOf(peso));
            opciones.add(fila);
        }
        model.addAttribute("pacientesConversion", opciones);

        // Medicamentos con rango de dosis
        List<Map<String, String>> meds = new ArrayList<>();
        List<Document> medicamentos = mongoTemplate.find(new Query().with(Sort.by(Sort.Order.asc("nombre"))), Document.class, "medicamentos");

        for (Document m : medicamentos) {
            Object dosMin = m.get("dosisMinimaMgKg");
            Object dosMax = m.get("dosisMaximaMgKg");
            if (dosMin == null || dosMax == null) continue;

            Map<String, String> med = new LinkedHashMap<>();
            med.put("id", String.valueOf(m.get("_id")));
            med.put("nombre", String.valueOf(m.getOrDefault("nombre", "")));
            med.put("min", String.valueOf(dosMin));
            med.put("max", String.valueOf(dosMax));
            meds.add(med);
        }
        model.addAttribute("medicamentosConversion", meds);
        return "conversiones";
    }

    // =========================================
    // ANÁLISIS
    // =========================================
    @GetMapping("/analisis")
    public String analisis(Model model) {
        // 1. Totales Básicos
        model.addAttribute("totalPacientes", mongoTemplate.count(new Query(), "pacientes"));
        model.addAttribute("totalCitas", mongoTemplate.count(new Query(), "citas"));
        model.addAttribute("totalHistoriales", mongoTemplate.count(new Query(), "historiales"));
        model.addAttribute("totalMedicamentos", mongoTemplate.count(new Query(), "medicamentos"));

        // 2. Datos para gráficos existentes
        List<Document> citas = mongoTemplate.find(new Query(), Document.class, "citas");
        List<Document> historiales = mongoTemplate.find(new Query(), Document.class, "historiales");
        List<Document> pacientes = mongoTemplate.find(new Query(), Document.class, "pacientes");

        Map<String, Long> porEstado = new LinkedHashMap<>();
        long programadas = 0, confirmadas = 0, atendidas = 0, canceladas = 0;

        for (Document cita : citas) {
            String estado = String.valueOf(cita.getOrDefault("estado", "SIN ESTADO")).trim().toUpperCase(Locale.ROOT);
            if (estado.isEmpty()) estado = "SIN ESTADO";
            porEstado.merge(estado, 1L, Long::sum);

            if (estado.contains("PROGRAM") || estado.contains("PENDIENT")) programadas++;
            if (estado.contains("CONFIRM")) confirmadas++;
            if (estado.contains("ATEND") || estado.contains("COMPLET") || estado.contains("FINALIZ")) atendidas++;
            if (estado.contains("CANCEL")) canceladas++;
        }
        model.addAttribute("citasPorEstado", porEstado);
        model.addAttribute("citasProgramadas", programadas);
        model.addAttribute("citasConfirmadas", confirmadas);
        model.addAttribute("citasAtendidas", atendidas);
        model.addAttribute("citasCanceladas", canceladas);

        // Actividad últimos 7 días
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
        List<String> dias = new ArrayList<>();
        List<Long> citasSemana = new ArrayList<>();
        List<Long> historialesSemana = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate fecha = hoy.minusDays(i);
            dias.add(fecha.toString());
            citasSemana.add(contarEnFecha(citas, "fecha", fecha));
            historialesSemana.add(contarEnFecha(historiales, "fecha", fecha));
        }
        model.addAttribute("actividadDias", dias);
        model.addAttribute("actividadCitas", citasSemana);
        model.addAttribute("actividadHistoriales", historialesSemana);

        // Actividad últimos 6 meses
        List<String> meses = new ArrayList<>();
        List<Long> pacientesPorMes = new ArrayList<>();
        List<Long> historialesPorMes = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth mes = YearMonth.from(hoy.minusMonths(i));
            meses.add(mes.toString());
            pacientesPorMes.add(contarEnMes(pacientes, "fechaRegistro", mes));
            historialesPorMes.add(contarEnMes(historiales, "fecha", mes));
        }
        model.addAttribute("meses", meses);
        model.addAttribute("pacientesPorMes", pacientesPorMes);
        model.addAttribute("historialesPorMes", historialesPorMes);

        // =========================================
        // 3. NUEVO: AUDITORÍA CLÍNICA (Validaciones de dosis)
        // =========================================
        long totalValidaciones = mongoTemplate.count(new Query(), "historial_conversiones");
        long totalSeguras = mongoTemplate.count(new Query(Criteria.where("estado").is("SEGURA")), "historial_conversiones");
        long totalInfradosis = mongoTemplate.count(new Query(Criteria.where("estado").is("INFRADOSIS")), "historial_conversiones");
        long totalSobredosis = mongoTemplate.count(new Query(Criteria.where("estado").is("SOBREDOSIS")), "historial_conversiones");

        model.addAttribute("totalValidaciones", totalValidaciones);
        model.addAttribute("totalSeguras", totalSeguras);
        model.addAttribute("totalInfradosis", totalInfradosis);
        model.addAttribute("totalSobredosis", totalSobredosis);

        // Obtener las últimas 10 validaciones para la tabla de auditoría
        Query queryRecientes = new Query().with(Sort.by(Sort.Order.desc("fecha"))).limit(10);
        List<Document> validacionesRecientes = mongoTemplate.find(queryRecientes, Document.class, "historial_conversiones");
        
        // Formatear las fechas de la auditoría para que se vean bonitas en el HTML
        List<Map<String, String>> auditoriaFormateada = new ArrayList<>();
        for (Document doc : validacionesRecientes) {
            Map<String, String> fila = new LinkedHashMap<>();
            fila.put("usuario", String.valueOf(doc.getOrDefault("usuario", "Desconocido")));
            fila.put("dosisRecetada", String.valueOf(doc.getOrDefault("dosisRecetada", "0")));
            fila.put("estado", String.valueOf(doc.getOrDefault("estado", "N/A")));
            // Formatear fecha
            String fechaStr = String.valueOf(doc.getOrDefault("fecha", ""));
            try {
                if (!fechaStr.isEmpty()) {
                    LocalDate fechaParseada = leerFecha(fechaStr); // Usa tu método existente
                    if (fechaParseada != null) {
                        fila.put("fecha", fechaParseada.toString());
                    } else {
                        fila.put("fecha", fechaStr.substring(0, Math.min(10, fechaStr.length())));
                    }
                } else {
                    fila.put("fecha", "Sin fecha");
                }
            } catch (Exception e) {
                fila.put("fecha", fechaStr.length() > 10 ? fechaStr.substring(0, 10) : fechaStr);
            }
            auditoriaFormateada.add(fila);
        }
        model.addAttribute("validacionesRecientes", auditoriaFormateada);

        return "analisis";
    }

    // =========================================
    // MÉTODOS AUXILIARES
    // =========================================
    private long contarEnFecha(List<Document> documentos, String campo, LocalDate fecha) {
        return documentos.stream().filter(d -> fecha.equals(leerFecha(d.get(campo)))).count();
    }

    private long contarEnMes(List<Document> documentos, String campo, YearMonth mes) {
        return documentos.stream().map(d -> leerFecha(d.get(campo))).filter(f -> f != null && YearMonth.from(f).equals(mes)).count();
    }

    private LocalDate leerFecha(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Date) return ((Date) valor).toInstant().atZone(ZoneId.of("America/Bogota")).toLocalDate();
        
        String texto = valor.toString().trim();
        if (texto.length() >= 10) {
            try { return LocalDate.parse(texto.substring(0, 10)); } catch (DateTimeParseException ignorado) {}
        }
        try { return Instant.parse(texto).atZone(ZoneId.of("America/Bogota")).toLocalDate(); } catch (DateTimeParseException ignorado) { return null; }
    }

    @GetMapping("/reportes")
    public String reportes(Model model) {
        model.addAttribute("totalPacientes", mongoTemplate.count(new Query(), "pacientes"));
        model.addAttribute("totalCitas", mongoTemplate.count(new Query(), "citas"));
        model.addAttribute("totalHistoriales", mongoTemplate.count(new Query(), "historiales"));
        return "reportes";
    }
}