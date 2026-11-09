package com.mycompany.clinica.service;

import com.mycompany.clinica.entity.Paciente;
import com.mycompany.clinica.repository.PacienteRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.regex.Pattern;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final MongoTemplate mongoTemplate;

    public PacienteService(
            PacienteRepository pacienteRepository,
            MongoTemplate mongoTemplate) {

        this.pacienteRepository = pacienteRepository;
        this.mongoTemplate = mongoTemplate;
    }

    // ============================================
    // LISTAR PACIENTES
    // ============================================

    public Page<Paciente> listarPacientes(
            int pagina,
            int cantidad) {

        return listarPacientes(pagina, cantidad, null);
    }

    public Page<Paciente> listarPacientes(
            int pagina,
            int cantidad,
            String buscar) {

        int paginaSegura = Math.max(0, pagina);
        int cantidadSegura = Math.max(1, Math.min(cantidad, 100));

        Pageable pageable = PageRequest.of(
                paginaSegura,
                cantidadSegura,
                Sort.by(
                        Sort.Order.desc("fechaRegistro"),
                        Sort.Order.desc("id")
                )
        );

        String termino = buscar == null
                ? ""
                : buscar.trim();

        if (termino.isEmpty()) {
            return pacienteRepository.findAll(pageable);
        }

        String expresion =
                ".*" + Pattern.quote(termino) + ".*";

        Criteria filtro = new Criteria().orOperator(
                Criteria.where("nombres").regex(expresion, "i"),
                Criteria.where("apellidos").regex(expresion, "i"),
                Criteria.where("documento").regex(expresion, "i"),
                Criteria.where("correo").regex(expresion, "i"),
                Criteria.where("telefono").regex(expresion, "i")
        );

        Query consulta = new Query(filtro);

        long total = mongoTemplate.count(
                consulta,
                Paciente.class
        );

        consulta.with(pageable);

        return new PageImpl<>(
                mongoTemplate.find(consulta, Paciente.class),
                pageable,
                total
        );
    }

    // ============================================
    // BUSCAR PACIENTE POR ID
    // ============================================

    public Paciente buscarPorId(String id) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "El ID del paciente es obligatorio."
            );
        }

        return pacienteRepository.findById(id.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Paciente no encontrado."
                        )
                );
    }

    // ============================================
    // REGISTRAR PACIENTE
    // ============================================

    public Paciente guardarPaciente(Paciente paciente) {

        if (paciente == null) {
            throw new IllegalArgumentException(
                    "Los datos del paciente son obligatorios."
            );
        }

        if (paciente.getId() != null
                && !paciente.getId().isBlank()) {

            throw new IllegalArgumentException(
                    "Para modificar un paciente existente debes utilizar actualizarPaciente."
            );
        }

        validarDatosPaciente(paciente);
        validarDatosFisicos(paciente);

        String documento = normalizar(
                paciente.getDocumento()
        );

        if (pacienteRepository.existsByDocumento(documento)) {
            throw new IllegalArgumentException(
                    "Ya existe un paciente con ese documento."
            );
        }

        paciente.setId(null);
        paciente.setDocumento(documento);
        paciente.setFechaRegistro(Instant.now());
        paciente.setActivo(true);

        // El modelo incluye peso y estatura.
        // MongoDB guardará ambos campos cuando se proporcionen.

        return pacienteRepository.save(paciente);
    }

    // ============================================
    // ACTUALIZAR PACIENTE
    // ============================================

    public Paciente actualizarPaciente(
            String id,
            Paciente datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos del paciente son obligatorios."
            );
        }

        Paciente paciente = buscarPorId(id);

        // Mantener fecha de nacimiento si no se envía.
        if (datos.getFechaNacimiento() == null) {
            datos.setFechaNacimiento(
                    paciente.getFechaNacimiento()
            );
        }

        validarDatosPaciente(datos);
        validarDatosFisicos(datos);

        String documentoNuevo = normalizar(
                datos.getDocumento()
        );

        String documentoActual = normalizar(
                paciente.getDocumento()
        );

        if (!documentoNuevo.equals(documentoActual)
                && pacienteRepository.existsByDocumento(
                        documentoNuevo)) {

            throw new IllegalArgumentException(
                    "Ya existe otro paciente con ese documento."
            );
        }

        // ========================================
        // INFORMACIÓN PERSONAL
        // ========================================

        paciente.setTipoDocumento(
                normalizar(datos.getTipoDocumento())
        );

        paciente.setDocumento(documentoNuevo);

        paciente.setNombres(
                normalizar(datos.getNombres())
        );

        paciente.setApellidos(
                normalizar(datos.getApellidos())
        );

        paciente.setFechaNacimiento(
                datos.getFechaNacimiento()
        );

        paciente.setSexo(datos.getSexo());

        paciente.setTipoSangre(datos.getTipoSangre());

        // ========================================
        // DATOS FÍSICOS
        // ========================================

        // Si no se envía un valor, conservar el anterior.

        if (datos.getPeso() != null) {
            paciente.setPeso(datos.getPeso());
        }

        if (datos.getEstatura() != null) {
            paciente.setEstatura(datos.getEstatura());
        }

        // El IMC se calcula automáticamente
        // mediante paciente.getImc().
        // No es necesario guardarlo por separado.

        // ========================================
        // INFORMACIÓN DE CONTACTO
        // ========================================

        paciente.setEps(
                normalizar(datos.getEps())
        );

        paciente.setTelefono(
                normalizar(datos.getTelefono())
        );

        paciente.setCorreo(
                normalizar(datos.getCorreo())
        );

        paciente.setDireccion(
                normalizar(datos.getDireccion())
        );

        paciente.setContactoEmergencia(
                normalizar(datos.getContactoEmergencia())
        );

        paciente.setTelefonoEmergencia(
                normalizar(datos.getTelefonoEmergencia())
        );

        // ========================================
        // ESTADO DEL PACIENTE
        // ========================================

        paciente.setActivo(datos.isActivo());

        // Conserva ID y fechaRegistro originales.

        return pacienteRepository.save(paciente);
    }

    // ============================================
    // VALIDAR INFORMACIÓN PERSONAL
    // ============================================

    private void validarDatosPaciente(Paciente paciente) {

        if (paciente.getNombres() == null
                || paciente.getNombres().isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre del paciente es obligatorio."
            );
        }

        if (paciente.getApellidos() == null
                || paciente.getApellidos().isBlank()) {

            throw new IllegalArgumentException(
                    "Los apellidos del paciente son obligatorios."
            );
        }

        if (paciente.getTipoDocumento() == null
                || paciente.getTipoDocumento().isBlank()) {

            throw new IllegalArgumentException(
                    "El tipo de documento es obligatorio."
            );
        }

        if (paciente.getDocumento() == null
                || paciente.getDocumento().isBlank()) {

            throw new IllegalArgumentException(
                    "El número de documento es obligatorio."
            );
        }

        if (paciente.getFechaNacimiento() == null) {
            throw new IllegalArgumentException(
                    "La fecha de nacimiento es obligatoria."
            );
        }

        if (!paciente.getFechaNacimiento()
                .isBefore(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "La fecha de nacimiento debe ser anterior a hoy."
            );
        }
    }

    // ============================================
    // VALIDAR PESO Y ESTATURA
    // ============================================

    private void validarDatosFisicos(Paciente paciente) {

        BigDecimal peso = paciente.getPeso();
        BigDecimal estatura = paciente.getEstatura();

        if (peso != null) {

            if (peso.compareTo(new BigDecimal("0.01")) < 0
                    || peso.compareTo(new BigDecimal("500.00")) > 0
                    || peso.scale() > 2) {

                throw new IllegalArgumentException(
                    "El peso debe estar entre 0.01 y 500 kg, con máximo dos decimales."
                );
            }
        }

        if (estatura != null) {

            if (estatura.compareTo(new BigDecimal("0.01")) < 0
                    || estatura.compareTo(new BigDecimal("300.00")) > 0
                    || estatura.scale() > 2) {

                throw new IllegalArgumentException(
                    "La estatura debe estar entre 0.01 y 300 cm, con máximo dos decimales."
                );
            }
        }
    }

    // ============================================
    // NORMALIZAR TEXTO
    // ============================================

    private String normalizar(String texto) {
        return texto == null ? "" : texto.trim();
    }

    // ============================================
    // ELIMINAR PACIENTE
    // ============================================

    public void eliminarPaciente(String id) {

        Paciente paciente = buscarPorId(id);

        pacienteRepository.deleteById(
                paciente.getId()
        );
    }

    // ============================================
    // CONTAR PACIENTES
    // ============================================

    public long contarPacientes() {
        return pacienteRepository.count();
    }
}
