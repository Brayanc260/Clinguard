package com.mycompany.clinica.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
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
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/recepcion/reportes")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
public class RecepcionReporteController {
    private final MongoTemplate mongo;
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    public RecepcionReporteController(MongoTemplate mongo) { this.mongo = mongo; }

    @GetMapping
    public String ver(@RequestParam(required = false) String desde,
                      @RequestParam(required = false) String hasta, Model model) {
        LocalDate[] rango = rango(desde, hasta);
        Resumen r = consultar(rango[0], rango[1]);
        model.addAttribute("desde", rango[0]);
        model.addAttribute("hasta", rango[1]);
        model.addAttribute("totalPacientes", mongo.count(new Query(), "pacientes"));
        model.addAttribute("totalCitas", r.total);
        model.addAttribute("pendientes", r.pendientes);
        model.addAttribute("confirmadas", r.confirmadas);
        model.addAttribute("canceladas", r.canceladas);
        model.addAttribute("porMedico", r.porMedico.entrySet());
        model.addAttribute("porFecha", r.porFecha.entrySet());
        return "recepcion-reportes";
    }

    @GetMapping("/exportar")
    public void exportar(@RequestParam(required = false) String desde,
                         @RequestParam(required = false) String hasta,
                         HttpServletResponse respuesta) throws IOException {
        LocalDate[] fechas = rango(desde, hasta);
        Resumen r = consultar(fechas[0], fechas[1]);
        respuesta.setContentType("text/csv; charset=UTF-8");
        respuesta.setHeader("Content-Disposition", "attachment; filename=recepcion-citas.csv");
        var escritor = respuesta.getWriter();
        escritor.write("\uFEFFFecha,Hora,MÃ©dico,Usuario,Estado\r\n");
        for (Map<String,String> cita : r.registros) {
            escritor.write(csv(cita.get("fecha")) + "," + csv(cita.get("hora")) + ","
                + csv(cita.get("medico")) + "," + csv(cita.get("usuario")) + ","
                + csv(cita.get("estado")) + "\r\n");
        }
        escritor.flush();
    }

    private Resumen consultar(LocalDate desde, LocalDate hasta) {
        Query query = new Query();
        query.fields().include("fecha").include("hora").include("estado")
            .include("medicoUsername").include("usuarioUsername");
        List<Document> docs = mongo.find(query, Document.class, "citas");
        Resumen r = new Resumen();
        for (Document d : docs) {
            LocalDate fecha;
            try { fecha = LocalDate.parse(valor(d, "fecha")); }
            catch (DateTimeParseException ex) { continue; }
            if (fecha.isBefore(desde) || fecha.isAfter(hasta)) continue;
            String estado = valor(d,"estado").toUpperCase(Locale.ROOT).trim();
            String medico = valor(d,"medicoUsername");
            r.registros.add(Map.of("fecha", fecha.toString(), "hora", valor(d,"hora"),
                "medico", medico, "usuario", valor(d,"usuarioUsername"), "estado", estado));
            r.total++;
            if (estado.equals("CANCELADA") || estado.equals("CANCELADO")) r.canceladas++;
            else if (estado.equals("CONFIRMADA")) r.confirmadas++;
            else if (estado.equals("PROGRAMADA") || estado.equals("PENDIENTE")) r.pendientes++;
            r.porFecha.merge(fecha.toString(), 1L, Long::sum);
            if (!medico.isBlank()) r.porMedico.merge(medico, 1L, Long::sum);
        }
        r.registros.sort((a,b) -> (a.get("fecha")+a.get("hora")).compareTo(b.get("fecha")+b.get("hora")));
        return r;
    }

    private LocalDate[] rango(String desde, String hasta) {
        LocalDate hoy = LocalDate.now(ZONA);
        LocalDate d = hoy.withDayOfMonth(1), h = hoy;
        try { if (desde != null && !desde.isBlank()) d = LocalDate.parse(desde); }
        catch (DateTimeParseException ignored) { }
        try { if (hasta != null && !hasta.isBlank()) h = LocalDate.parse(hasta); }
        catch (DateTimeParseException ignored) { }
        if (d.isAfter(h)) { LocalDate temporal = d; d = h; h = temporal; }
        return new LocalDate[]{d,h};
    }

    private String valor(Document d, String campo) {
        Object v = d.get(campo);
        return v == null ? "" : v.toString();
    }

    private String csv(String v) {
        String s = v == null ? "" : v;
        // Evitar fÃ³rmulas al abrir el CSV en hojas de cÃ¡lculo.
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static class Resumen {
        long total, pendientes, confirmadas, canceladas;
        final Map<String,Long> porMedico = new TreeMap<>();
        final Map<String,Long> porFecha = new TreeMap<>();
        final List<Map<String,String>> registros = new ArrayList<>();
    }
}

