package com.mycompany.clinica.service;

import com.mycompany.clinica.entity.Paciente;
import com.mycompany.clinica.repository.PacienteRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class PacienteService {

        private final PacienteRepository pacienteRepository;
        private final MongoTemplate mongoTemplate;

        public PacienteService(PacienteRepository pacienteRepository, MongoTemplate mongoTemplate) {
                this.pacienteRepository = pacienteRepository;
                this.mongoTemplate = mongoTemplate;
        }

        // Listar nuevos primero y buscar en toda la colecciÃ³n, no solo en la pÃ¡gina
        // actual.
        public Page<Paciente> listarPacientes(int pagina, int cantidad) {
                return listarPacientes(pagina, cantidad, null);
        }

        public Page<Paciente> listarPacientes(int pagina, int cantidad, String buscar) {
                int paginaSegura = Math.max(0, pagina);
                int cantidadSegura = Math.max(1, Math.min(cantidad, 100));
                Pageable pageable = PageRequest.of(paginaSegura, cantidadSegura,
                                Sort.by(Sort.Order.desc("fechaRegistro"), Sort.Order.desc("id")));
                String termino = buscar == null ? "" : buscar.trim();
                if (termino.isEmpty()) {
                        return pacienteRepository.findAll(pageable);
                }
                // Pattern.quote impide interpretar el texto introducido como regex.
                String expresion = ".*" + Pattern.quote(termino) + ".*";
                Criteria filtro = new Criteria().orOperator(
                                Criteria.where("nombres").regex(expresion, "i"),
                                Criteria.where("apellidos").regex(expresion, "i"),
                                Criteria.where("documento").regex(expresion, "i"),
                                Criteria.where("correo").regex(expresion, "i"),
                                Criteria.where("telefono").regex(expresion, "i"));
                Query consulta = new Query(filtro);
                long total = mongoTemplate.count(consulta, Paciente.class);
                consulta.with(pageable);
                return new PageImpl<>(mongoTemplate.find(consulta, Paciente.class), pageable, total);
        }

        public Paciente buscarPorId(String id) {
                if (id == null || id.isBlank()) {
                        throw new IllegalArgumentException("El ID del paciente es obligatorio.");
                }
                return pacienteRepository.findById(id.trim())
                                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado."));
        }

        public Paciente guardarPaciente(Paciente paciente) {
                if (paciente == null) {
                        throw new IllegalArgumentException("Los datos del paciente son obligatorios.");
                }
                if (paciente.getId() != null && !paciente.getId().isBlank()) {
                        throw new IllegalArgumentException(
                                        "Para modificar un paciente existente debes utilizar el mÃ©todo actualizarPaciente.");
                }

                validarDatosPaciente(paciente);
                String documento = normalizar(paciente.getDocumento());
                if (pacienteRepository.existsByDocumento(documento)) {
                        throw new IllegalArgumentException("Ya existe un paciente con ese documento.");
                }

                paciente.setId(null);
                paciente.setDocumento(documento);
                paciente.setFechaRegistro(Instant.now());
                paciente.setActivo(true);
                return pacienteRepository.save(paciente);
        }

        public Paciente actualizarPaciente(String id, Paciente datos) {
                if (datos == null) {
                        throw new IllegalArgumentException("Los datos del paciente son obligatorios.");
                }

                Paciente paciente = buscarPorId(id);

                // Si el formulario omite la fecha de nacimiento, conservar la ya registrada.
                // Si se envÃ­a una fecha distinta, validarla y actualizarla normalmente.
                if (datos.getFechaNacimiento() == null) {
                        datos.setFechaNacimiento(paciente.getFechaNacimiento());
                }
                validarDatosPaciente(datos);

                String documentoNuevo = normalizar(datos.getDocumento());
                String documentoActual = normalizar(paciente.getDocumento());
                if (!documentoNuevo.equals(documentoActual)
                                && pacienteRepository.existsByDocumento(documentoNuevo)) {
                        throw new IllegalArgumentException("Ya existe otro paciente con ese documento.");
                }

                paciente.setTipoDocumento(normalizar(datos.getTipoDocumento()));
                paciente.setDocumento(documentoNuevo);
                paciente.setNombres(normalizar(datos.getNombres()));
                paciente.setApellidos(normalizar(datos.getApellidos()));
                paciente.setFechaNacimiento(datos.getFechaNacimiento());
                paciente.setSexo(datos.getSexo());
                paciente.setTipoSangre(datos.getTipoSangre());
                paciente.setEps(normalizar(datos.getEps()));
                paciente.setTelefono(normalizar(datos.getTelefono()));
                paciente.setCorreo(normalizar(datos.getCorreo()));
                paciente.setDireccion(normalizar(datos.getDireccion()));
                paciente.setContactoEmergencia(normalizar(datos.getContactoEmergencia()));
                paciente.setTelefonoEmergencia(normalizar(datos.getTelefonoEmergencia()));

                // Aplicar el estado elegido explÃ­citamente en el formulario de ediciÃ³n.
                // Asegurar que paciente-form.html envÃ­e siempre activo=true o activo=false
                // mediante un <select th:field="*{activo}">.
                paciente.setActivo(datos.isActivo());

                return pacienteRepository.save(paciente);
        }

        private void validarDatosPaciente(Paciente paciente) {
                if (paciente.getNombres() == null || paciente.getNombres().isBlank()) {
                        throw new IllegalArgumentException("El nombre del paciente es obligatorio.");
                }
                if (paciente.getApellidos() == null || paciente.getApellidos().isBlank()) {
                        throw new IllegalArgumentException("Los apellidos del paciente son obligatorios.");
                }
                if (paciente.getTipoDocumento() == null || paciente.getTipoDocumento().isBlank()) {
                        throw new IllegalArgumentException("El tipo de documento es obligatorio.");
                }
                if (paciente.getDocumento() == null || paciente.getDocumento().isBlank()) {
                        throw new IllegalArgumentException("El nÃºmero de documento es obligatorio.");
                }
                if (paciente.getFechaNacimiento() == null) {
                        throw new IllegalArgumentException("La fecha de nacimiento es obligatoria.");
                }
                if (paciente.getFechaNacimiento().isAfter(LocalDate.now())) {
                        throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
                }
        }

        private String normalizar(String texto) {
                return texto == null ? "" : texto.trim();
        }

        // MÃ©todo original conservado por compatibilidad. Su uso debe quedar restringido
        // al administrador y revisarse antes de borrar pacientes con citas o
        // historiales.
        public void eliminarPaciente(String id) {
                Paciente paciente = buscarPorId(id);
                pacienteRepository.deleteById(paciente.getId());
        }

        public long contarPacientes() {
                return pacienteRepository.count();
        }
}
