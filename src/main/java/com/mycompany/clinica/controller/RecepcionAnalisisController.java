
package com.mycompany.clinica.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.bson.Document;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/recepcion/analisis")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
public class RecepcionAnalisisController {

    private final MongoTemplate mongoTemplate;

    public RecepcionAnalisisController(
            MongoTemplate mongoTemplate) {

        this.mongoTemplate = mongoTemplate;
    }

    // =====================================================
    // ANÃLISIS ADMINISTRATIVO DE RECEPCIÃ“N
    // =====================================================

    @GetMapping
    public String mostrarAnalisis(Model model) {

        // =============================================
        // TOTAL DE PACIENTES
        // =============================================

        long totalPacientes = mongoTemplate.count(
                new Query(),
                "pacientes"
        );

        // =============================================
        // CONSULTAR CITAS
        // =============================================

        Query query = new Query();

        // Solo informaciÃ³n administrativa
        query.fields()
                .include("fecha")
                .include("hora")
                .include("estado")
                .include("medicoUsername");

        List<Document> citas = mongoTemplate.find(
                query,
                Document.class,
                "citas"
        );

        long totalCitas = citas.size();
        long citasPendientes = 0;
        long citasConfirmadas = 0;
        long citasCanceladas = 0;

        // Agrupaciones para las grÃ¡ficas
        Map<String, Long> citasPorMedico =
                new TreeMap<>();

        Map<String, Long> citasPorFecha =
                new TreeMap<>();

        Map<String, Long> citasPorHora =
                new TreeMap<>();

        for (Document cita : citas) {

            // =========================================
            // ESTADOS
            // =========================================

            String estado = obtenerTexto(
                    cita,
                    "estado"
            ).trim().toUpperCase(Locale.ROOT);

            if ("PROGRAMADA".equals(estado)
                    || "PENDIENTE".equals(estado)) {

                citasPendientes++;
            }

            if ("CONFIRMADA".equals(estado)) {
                citasConfirmadas++;
            }

            if ("CANCELADA".equals(estado)
                    || "CANCELADO".equals(estado)) {

                citasCanceladas++;
            }

            // No incluir canceladas en las
            // estadÃ­sticas de ocupaciÃ³n.
            boolean citaActiva =
                    !"CANCELADA".equals(estado)
                    && !"CANCELADO".equals(estado);

            if (citaActiva) {

                String medico = obtenerTexto(
                        cita,
                        "medicoUsername"
                );

                if (!medico.isBlank()) {

                    citasPorMedico.merge(
                            medico,
                            1L,
                            Long::sum
                    );
                }

                String fecha = obtenerTexto(
                        cita,
                        "fecha"
                );

                if (!fecha.isBlank()) {

                    citasPorFecha.merge(
                            fecha,
                            1L,
                            Long::sum
                    );
                }

                String hora = obtenerTexto(
                        cita,
                        "hora"
                );

                if (!hora.isBlank()) {

                    citasPorHora.merge(
                            hora,
                            1L,
                            Long::sum
                    );
                }
            }
        }

        // =============================================
        // PORCENTAJE DE CANCELACIÃ“N
        // =============================================

        double porcentajeCancelacion =
                totalCitas == 0
                        ? 0.0
                        : (citasCanceladas * 100.0)
                            / totalCitas;

        // =============================================
        // DATOS PARA LA VISTA THYMELEAF
        // =============================================

        model.addAttribute(
                "totalPacientes",
                totalPacientes
        );

        model.addAttribute(
                "totalCitas",
                totalCitas
        );

        model.addAttribute(
                "citasPendientes",
                citasPendientes
        );

        model.addAttribute(
                "citasConfirmadas",
                citasConfirmadas
        );

        model.addAttribute(
                "citasCanceladas",
                citasCanceladas
        );

        model.addAttribute(
                "porcentajeCancelacion",
                Math.round(porcentajeCancelacion * 10.0) / 10.0
        );

        model.addAttribute(
                "citasPorMedico",
                crearDatosGrafica(citasPorMedico)
        );

        model.addAttribute(
                "citasPorFecha",
                crearDatosGrafica(citasPorFecha)
        );

        model.addAttribute(
                "citasPorHora",
                crearDatosGrafica(citasPorHora)
        );

        return "recepcion-analisis";
    }

    // =====================================================
    // CONVERTIR DOCUMENTO A TEXTO
    // =====================================================

    private String obtenerTexto(
            Document documento,
            String campo) {

        Object valor = documento.get(campo);

        return valor == null
                ? ""
                : valor.toString();
    }

    // =====================================================
    // PREPARAR DATOS PARA GRÃFICAS
    // =====================================================

    private List<Map<String, Object>> crearDatosGrafica(
            Map<String, Long> datos) {

        List<Map<String, Object>> resultado =
                new ArrayList<>();

        datos.forEach((nombre, cantidad) -> {

            resultado.add(
                    Map.of(
                            "nombre", nombre,
                            "cantidad", cantidad
                    )
            );
        });

        return resultado;
    }
}

