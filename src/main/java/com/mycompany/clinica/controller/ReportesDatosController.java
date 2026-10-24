
package com.mycompany.clinica.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reportes")
public class ReportesDatosController {

    private final MongoTemplate mongoTemplate;
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    public ReportesDatosController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/datos")
    public ResponseEntity<?> datos(
            @RequestParam(defaultValue = "general") String tipo,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            Authentication auth) {

        boolean esAdmin = tieneRol(auth, "ROLE_ADMIN");
        boolean esMedico = tieneRol(auth, "ROLE_MEDICO");

        if (!esAdmin && !esMedico) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",
                            "No tienes permiso para consultar estos datos."));
        }

        if (!List.of(
                "general",
                "pacientes",
                "citas",
                "historiales",
                "conversiones").contains(tipo)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Tipo de reporte no vÃ¡lido."));
        }

        // Solo ADMIN puede consultar el reporte
        // completo de pacientes.
        if (esMedico && !esAdmin && "pacientes".equals(tipo)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",
                            "El reporte completo de pacientes estÃ¡ reservado al administrador."));
        }

        LocalDate inicio;
        LocalDate fin;

        try {
            inicio = desde == null || desde.isBlank()
                    ? null
                    : LocalDate.parse(desde);

            fin = hasta == null || hasta.isBlank()
                    ? null
                    : LocalDate.parse(hasta);

        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Formato de fecha incorrecto."));
        }

        if (inicio != null && fin != null
                && inicio.isAfter(fin)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "La fecha Desde no puede ser posterior a Hasta."));
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("tipo", tipo);
        respuesta.put("desde", desde == null ? "" : desde);
        respuesta.put("hasta", hasta == null ? "" : hasta);

        List<Map<String, String>> filas = new ArrayList<>();

        long pacientes = 0;
        long citas = 0;
        long historiales = 0;
        long conversiones = 0;

        // =========================================
        // ÃNDICE DE PACIENTES
        // =========================================

        Map<String, Document> pacientesPorReferencia = new HashMap<>();

        Set<String> referenciasAmbiguas = new HashSet<>();

        if (!tipo.equals("pacientes")) {

            for (Document p : mongoTemplate.find(
                    new Query(),
                    Document.class,
                    "pacientes")) {

                agregarReferencia(
                        pacientesPorReferencia,
                        referenciasAmbiguas,
                        texto(p.get("_id")),
                        p);

                agregarReferencia(
                        pacientesPorReferencia,
                        referenciasAmbiguas,
                        texto(p.get("documento")),
                        p);
            }
        }

        Set<String> pacientesAtendidos = new HashSet<>();

        // =========================================
        // REPORTE DE CITAS
        // =========================================

        if (tipo.equals("general") || tipo.equals("citas")) {

            List<Document> lista = mongoTemplate.find(
                    new Query(),
                    Document.class,
                    "citas");

            for (Document c : lista) {

                if (!esAdmin && !auth.getName().equals(
                        texto(c.get("medicoUsername")))) {
                    continue;
                }

                if (!enRango(
                        c.get("fecha"), inicio, fin)) {
                    continue;
                }

                citas++;

                if (!esAdmin) {
                    agregarPacienteVinculado(
                            c,
                            pacientesPorReferencia,
                            referenciasAmbiguas,
                            pacientesAtendidos);
                }

                if (tipo.equals("citas")) {

                    Map<String, String> r = new LinkedHashMap<>();

                    r.put("Fecha",
                            fechaTexto(c.get("fecha")));

                    r.put("Hora",
                            texto(c.get("hora")));

                    r.put("Paciente",
                            nombrePaciente(
                                    c,
                                    pacientesPorReferencia,
                                    referenciasAmbiguas));

                    r.put("MÃ©dico",
                            texto(c.get("medicoUsername")));

                    r.put("Estado",
                            texto(c.get("estado")));

                    filas.add(r);
                }
            }
        }

        // =========================================
        // REPORTE DE HISTORIALES
        // =========================================

        if (tipo.equals("general")
                || tipo.equals("historiales")) {

            List<Document> lista = mongoTemplate.find(
                    new Query(),
                    Document.class,
                    "historiales");

            for (Document h : lista) {

                if (!esAdmin && !auth.getName().equals(
                        texto(h.get("medicoUsername")))) {
                    continue;
                }

                if (!enRango(
                        h.get("fecha"), inicio, fin)) {
                    continue;
                }

                historiales++;

                if (!esAdmin) {
                    agregarPacienteVinculado(
                            h,
                            pacientesPorReferencia,
                            referenciasAmbiguas,
                            pacientesAtendidos);
                }

                if (tipo.equals("historiales")) {

                    Map<String, String> r = new LinkedHashMap<>();

                    r.put("Fecha",
                            fechaTexto(h.get("fecha")));

                    r.put("Paciente",
                            nombrePaciente(
                                    h,
                                    pacientesPorReferencia,
                                    referenciasAmbiguas));

                    r.put("MÃ©dico",
                            texto(h.get("medicoUsername")));

                    // No exportar diagnÃ³sticos, tratamientos
                    // ni observaciones clÃ­nicas.
                    filas.add(r);
                }
            }
        }

        // =========================================
        // REPORTE DE PACIENTES
        // =========================================

        if (esAdmin && (tipo.equals("general")
                || tipo.equals("pacientes"))) {

            List<Document> lista = mongoTemplate.find(
                    new Query(),
                    Document.class,
                    "pacientes");

            for (Document p : lista) {

                if (!enRango(
                        p.get("fechaRegistro"), inicio, fin)) {
                    continue;
                }

                pacientes++;

                if (tipo.equals("pacientes")) {

                    Map<String, String> r = new LinkedHashMap<>();

                    r.put("Paciente",
                            unir(
                                    p.get("nombres"),
                                    p.get("apellidos")));

                    r.put("Documento",
                            texto(p.get("documento")));

                    r.put("Fecha de registro",
                            fechaTexto(
                                    p.get("fechaRegistro")));

                    r.put("Estado",
                            Boolean.FALSE.equals(
                                    p.get("activo"))
                                            ? "Inactivo"
                                            : "Activo");

                    filas.add(r);
                }
            }

        } else if (!esAdmin && tipo.equals("general")) {
            pacientes = pacientesAtendidos.size();
        }

        // =========================================
        // REPORTE DE CONVERSIONES MÃ‰DICAS
        // =========================================

        if (tipo.equals("general")
                || tipo.equals("conversiones")) {

            List<Document> lista = mongoTemplate.find(
                    new Query(),
                    Document.class,
                    "conversiones_medicas");

            for (Document d : lista) {

                // El administrador consulta todas.
                // El mÃ©dico consulta solamente las suyas.
                if (!esAdmin && !auth.getName().equals(
                        texto(d.get("usuarioUsername")))) {
                    continue;
                }

                if (!enRango(
                        d.get("fecha"), inicio, fin)) {
                    continue;
                }

                conversiones++;

                if (tipo.equals("conversiones")) {

                    Map<String, String> r = new LinkedHashMap<>();

                    r.put("Fecha",
                            fechaTexto(d.get("fecha")));

                    r.put("Paciente",
                            texto(d.get("pacienteNombre")));

                    r.put("Documento",
                            texto(d.get("pacienteDocumento")));

                    r.put("Profesional",
                            texto(d.get("usuarioUsername")));

                    r.put("ConversiÃ³n",
                            texto(d.get("operacion")));

                    r.put("Valor inicial",
                            texto(d.get("valor")));

                    r.put("Resultado",
                            texto(d.get("resultado"))
                                    + " "
                                    + texto(d.get("unidad")));

                    filas.add(r);
                }
            }
        }

        // =========================================
        // REPORTE GENERAL
        // =========================================

        if (tipo.equals("general")) {

            String[][] datos = {
                    {
                            esAdmin
                                    ? "Pacientes registrados"
                                    : "Pacientes vinculados a mis registros",
                            String.valueOf(pacientes)
                    },
                    {
                            esAdmin
                                    ? "Citas registradas"
                                    : "Mis citas",
                            String.valueOf(citas)
                    },
                    {
                            esAdmin
                                    ? "Historiales registrados"
                                    : "Mis historiales",
                            String.valueOf(historiales)
                    },
                    {
                            esAdmin
                                    ? "Conversiones registradas"
                                    : "Mis conversiones",
                            String.valueOf(conversiones)
                    }
            };

            for (String[] dato : datos) {

                Map<String, String> r = new LinkedHashMap<>();

                r.put("CategorÃ­a", dato[0]);
                r.put("Total", dato[1]);

                filas.add(r);
            }
        }

        // =========================================
        // RESPUESTA JSON
        // =========================================

        respuesta.put("totalPacientes", pacientes);
        respuesta.put("totalCitas", citas);
        respuesta.put("totalHistoriales", historiales);
        respuesta.put("totalConversiones", conversiones);
        respuesta.put("filas", filas);

        return ResponseEntity.ok(respuesta);
    }

    // =========================================
    // SEGURIDAD
    // =========================================

    private boolean tieneRol(
            Authentication auth,
            String rol) {

        return auth != null
                && auth.isAuthenticated()
                && auth.getAuthorities().stream()
                        .anyMatch(a -> rol.equals(a.getAuthority()));
    }

    // =========================================
    // PACIENTES VINCULADOS
    // =========================================

    private void agregarPacienteVinculado(
            Document registro,
            Map<String, Document> indice,
            Set<String> ambiguas,
            Set<String> ids) {

        String id = texto(
                registro.get("pacienteId")).trim();

        String referencia = id.isBlank()
                ? texto(
                        registro.get("usuarioUsername")).trim()
                : id;

        if (referencia.isBlank()
                || ambiguas.contains(referencia)) {
            return;
        }

        Document p = indice.get(referencia);

        if (p != null) {
            ids.add(texto(p.get("_id")));
        }
    }

    private void agregarReferencia(
            Map<String, Document> indice,
            Set<String> ambiguas,
            String referencia,
            Document paciente) {

        if (referencia.isBlank()) {
            return;
        }

        Document previo = indice.putIfAbsent(
                referencia,
                paciente);

        if (previo != null
                && !texto(previo.get("_id")).equals(
                        texto(paciente.get("_id")))) {

            ambiguas.add(referencia);
        }
    }

    private String nombrePaciente(
            Document registro,
            Map<String, Document> indice,
            Set<String> ambiguas) {

        String id = texto(
                registro.get("pacienteId")).trim();

        String anterior = texto(
                registro.get("usuarioUsername")).trim();

        String referencia = !id.isBlank()
                ? id
                : anterior;

        if (referencia.isBlank()) {
            return "Sin paciente vinculado";
        }

        if (ambiguas.contains(referencia)) {
            return "Referencia ambigua (" + referencia + ")";
        }

        Document paciente = indice.get(referencia);

        if (paciente == null) {
            return "Referencia sin verificar ("
                    + referencia + ")";
        }

        String nombre = unir(
                paciente.get("nombres"),
                paciente.get("apellidos"));

        return nombre.isBlank()
                ? "Paciente sin nombre registrado"
                : nombre;
    }

    // =========================================
    // FILTRO POR FECHA
    // =========================================

    private boolean enRango(
            Object valor,
            LocalDate inicio,
            LocalDate fin) {

        if (inicio == null && fin == null) {
            return true;
        }

        LocalDate fecha = leerFecha(valor);

        return fecha != null
                && (inicio == null
                        || !fecha.isBefore(inicio))
                && (fin == null
                        || !fecha.isAfter(fin));
    }

    // =========================================
    // CONVERTIR FECHAS DE MONGODB
    // =========================================

    private LocalDate leerFecha(Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor instanceof Date d) {
            return d.toInstant()
                    .atZone(ZONA)
                    .toLocalDate();
        }

        if (valor instanceof Instant i) {
            return i.atZone(ZONA)
                    .toLocalDate();
        }

        String v = valor.toString();

        try {
            return LocalDate.parse(
                    v.substring(0, 10));

        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String fechaTexto(Object valor) {

        LocalDate fecha = leerFecha(valor);

        return fecha == null
                ? "Sin fecha"
                : fecha.toString();
    }

    // =========================================
    // FUNCIONES DE TEXTO
    // =========================================

    private String texto(Object valor) {
        return valor == null
                ? ""
                : String.valueOf(valor);
    }

    private String unir(Object a, Object b) {
        return (texto(a) + " " + texto(b)).trim();
    }
}

