package com.mycompany.clinica.service;

import com.mycompany.clinica.conversion.*;
import com.mycompany.clinica.dto.ConversionRequest;
import com.mycompany.clinica.dto.ConversionResponse;
import com.mycompany.clinica.dto.ValidacionDosisRequest;
import com.mycompany.clinica.entity.RegistroConversion;
import com.mycompany.clinica.entity.Paciente;
import com.mycompany.clinica.entity.Medicamento;
import com.mycompany.clinica.repository.MedicamentoRepository;
import com.mycompany.clinica.repository.PacienteRepository;
import com.mycompany.clinica.repository.RegistroConversionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversionService {

    private final RegistroConversionRepository registroRepo;
    private final PacienteRepository pacienteRepo;
    private final MedicamentoRepository medicamentoRepo;

    public ConversionService(RegistroConversionRepository registroRepo,
                             PacienteRepository pacienteRepo,
                             MedicamentoRepository medicamentoRepo) {
        this.registroRepo = registroRepo;
        this.pacienteRepo = pacienteRepo;
        this.medicamentoRepo = medicamentoRepo;
    }

    /**
     * Realiza conversiones simples y guarda el historial
     */
    public ConversionResponse convertir(ConversionRequest req, String usuarioNombre) {
        ConversionBase conversor = switch (req.categoria()) {
            case "Dosis y Peso"  -> new ConversionDosisPeso(req.tipoConversion(), req.valor());
            case "Volumen"       -> new ConversionVolumen(req.tipoConversion(), req.valor());
            case "Laboratorio"   -> new ConversionLaboratorio(req.tipoConversion(), req.valor());
            default -> throw new IllegalArgumentException("Categoría no válida: " + req.categoria());
        };

        conversor.convertir();

        RegistroConversion registro = new RegistroConversion();
        registro.setUsuarioNombre(usuarioNombre);
        registro.setTipoConversion(req.tipoConversion());
        registro.setValorEntrada(req.valor());
        registro.setValorSalida(conversor.getValorSalida());
        registro.setFechaHora(LocalDateTime.now());

        registroRepo.save(registro);

        return new ConversionResponse(
            req.tipoConversion(),
            req.valor(),
            conversor.getValorSalida(),
            conversor.getResultado()
        );
    }

    /**
     * Lógica de Validación Clínica:
     * Calcula si la dosis es segura basándose en el peso del paciente
     * y los rangos configurados para el medicamento en la base de datos.
     */
    public String validarDosisClinica(ValidacionDosisRequest req) {
        Paciente paciente = pacienteRepo.findById(req.pacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado con ID: " + req.pacienteId()));

        Medicamento medicamento = medicamentoRepo.findById(req.medicamentoId())
                .orElseThrow(() -> new RuntimeException("Medicamento no encontrado con ID: " + req.medicamentoId()));

        double peso = paciente.getPesoKg();
        double dosisMinimaSegura = peso * medicamento.getDosisMinimaMgKg();
        double dosisMaximaSegura = peso * medicamento.getDosisMaximaMgKg();

        if (req.dosisRecetada() < dosisMinimaSegura) {
            return "ALERTA: Infradosis. Para un peso de " + peso + "kg, la dosis mínima es " + dosisMinimaSegura + " mg.";
        } else if (req.dosisRecetada() > dosisMaximaSegura) {
            return "PELIGRO: Sobredosis. Para un peso de " + peso + "kg, el límite máximo es " + dosisMaximaSegura + " mg.";
        } else {
            return "DOSIS SEGURA: El rango para el paciente " + paciente.getNombre() + " es de " + dosisMinimaSegura + " a " + dosisMaximaSegura + " mg.";
        }
    }

    public List<RegistroConversion> obtenerHistorial(String usuarioNombre) {
        return registroRepo.findByUsuarioNombreOrderByFechaHoraDesc(usuarioNombre);
    }

    public org.springframework.data.domain.Page<RegistroConversion> obtenerHistorialPaginado(
            String usuarioNombre, int page, int size) {
        return registroRepo.findByUsuarioNombreOrderByFechaHoraDesc(
            usuarioNombre, org.springframework.data.domain.PageRequest.of(page, size));
    }

    public List<RegistroConversion> obtenerTodoElHistorial() {
        return registroRepo.findAll();
    }

    public org.springframework.data.domain.Page<RegistroConversion> obtenerTodoElHistorialPaginado(
            int page, int size) {
        return registroRepo.findAllByOrderByFechaHoraDesc(
            org.springframework.data.domain.PageRequest.of(page, size));
    }
}