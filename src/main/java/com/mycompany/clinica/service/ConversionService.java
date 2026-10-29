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

import java.math.BigDecimal;
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

    /** Realiza conversiones simples y guarda el historial. */
    public ConversionResponse convertir(ConversionRequest req, String usuarioNombre) {
        ConversionBase conversor = switch (req.categoria()) {
            case "Dosis y Peso" -> new ConversionDosisPeso(req.tipoConversion(), req.valor());
            case "Volumen" -> new ConversionVolumen(req.tipoConversion(), req.valor());
            case "Laboratorio" -> new ConversionLaboratorio(req.tipoConversion(), req.valor());
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
     * Comprueba matemáticamente una dosis introducida frente a los rangos
     * mg/kg configurados. No sustituye la verificación clínica profesional.
     */
    public String validarDosisClinica(ValidacionDosisRequest req) {
        Paciente paciente = pacienteRepo.findById(req.pacienteId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Paciente no encontrado con ID: " + req.pacienteId()));

        Medicamento medicamento = medicamentoRepo.findById(req.medicamentoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Medicamento no encontrado con ID: " + req.medicamentoId()));

        BigDecimal pesoPaciente = paciente.getPeso();
        if (pesoPaciente == null || pesoPaciente.signum() <= 0) {
            throw new IllegalArgumentException(
                    "El paciente no tiene un peso válido registrado. Registra el peso en su ficha antes de validar la dosis.");
        }

        double peso = pesoPaciente.doubleValue();
        double rangoMinimo = medicamento.getDosisMinimaMgKg();
        double rangoMaximo = medicamento.getDosisMaximaMgKg();
        double dosisRecetada = req.dosisRecetada();

        if (!Double.isFinite(peso) || !Double.isFinite(rangoMinimo)
                || !Double.isFinite(rangoMaximo) || !Double.isFinite(dosisRecetada)
                || rangoMinimo < 0 || rangoMaximo <= 0
                || rangoMinimo > rangoMaximo || dosisRecetada < 0) {
            throw new IllegalArgumentException(
                    "No se puede comparar la dosis: revisa la dosis ingresada y los rangos del medicamento.");
        }

        double dosisMinima = peso * rangoMinimo;
        double dosisMaxima = peso * rangoMaximo;
        String nombrePaciente = paciente.getNombreCompleto();

        if (dosisRecetada < dosisMinima) {
            return String.format(java.util.Locale.ROOT,
                    "ALERTA: Dosis por debajo del rango configurado. Paciente: %s; peso: %.2f kg; mínimo calculado: %.2f mg.",
                    nombrePaciente, peso, dosisMinima);
        } else if (dosisRecetada > dosisMaxima) {
            return String.format(java.util.Locale.ROOT,
                    "ALERTA: Dosis por encima del rango configurado. Paciente: %s; peso: %.2f kg; máximo calculado: %.2f mg.",
                    nombrePaciente, peso, dosisMaxima);
        } else {
            return String.format(java.util.Locale.ROOT,
                    "Dosis dentro del rango configurado para %s: %.2f a %.2f mg. Requiere validación clínica.",
                    nombrePaciente, dosisMinima, dosisMaxima);
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
