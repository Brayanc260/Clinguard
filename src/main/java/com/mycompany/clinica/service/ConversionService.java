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

    /** Realiza conversiones simples y guarda el historial. */
    public ConversionResponse convertir(ConversionRequest req, String usuarioNombre) {
        
        // CORRECCIÓN: Usar getCategoria(), getTipoConversion() y getValor()
        String categoria = req.getCategoria() != null ? req.getCategoria() : "Dosis y Peso";
        String tipo = req.getTipoConversion() != null ? req.getTipoConversion() : "";
        Double valor = req.getValor() != null ? req.getValor() : 0.0;

        ConversionBase conversor = switch (categoria) {
            case "Dosis y Peso" -> new ConversionDosisPeso(tipo, valor);
            case "Volumen" -> new ConversionVolumen(tipo, valor);
            case "Laboratorio" -> new ConversionLaboratorio(tipo, valor);
            default -> throw new IllegalArgumentException("Categoría no válida: " + categoria);
        };

        conversor.convertir();

        RegistroConversion registro = new RegistroConversion();
        registro.setUsuarioNombre(usuarioNombre);
        registro.setTipoConversion(tipo);
        registro.setValorEntrada(valor);
        registro.setValorSalida(conversor.getValorSalida());
        registro.setFechaHora(LocalDateTime.now());
        registroRepo.save(registro);

        return new ConversionResponse(
                tipo,
                valor,
                conversor.getValorSalida(),
                conversor.getResultado()
        );
    }

    /**
     * Comprueba matemáticamente una dosis introducida frente a los rangos mg/kg.
     */
    public String validarDosisClinica(ValidacionDosisRequest req) {
        // Nota: Asumo que ValidacionDosisRequest es un record, por eso usa req.pacienteId() sin get
        Paciente paciente = pacienteRepo.findById(req.pacienteId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Paciente no encontrado con ID: " + req.pacienteId()));

        Medicamento medicamento = medicamentoRepo.findById(req.medicamentoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Medicamento no encontrado con ID: " + req.medicamentoId()));

        if (paciente.getPeso() == null) {
            throw new IllegalArgumentException("El paciente no tiene un peso válido registrado.");
        }
        double peso = paciente.getPeso().doubleValue();
        
        if (peso <= 0) {
            throw new IllegalArgumentException("El paciente no tiene un peso válido registrado.");
        }

        double rangoMinimo = medicamento.getDosisMinimaMgKg() != null ? medicamento.getDosisMinimaMgKg().doubleValue() : 0;
        double rangoMaximo = medicamento.getDosisMaximaMgKg() != null ? medicamento.getDosisMaximaMgKg().doubleValue() : 0;
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
        
        String nombrePaciente;
        try {
            nombrePaciente = paciente.getNombreCompleto();
        } catch (Exception e) {
            nombrePaciente = paciente.getNombres() + " " + paciente.getApellidos();
        }

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