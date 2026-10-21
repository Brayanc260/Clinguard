package com.mycompany.clinica.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.clinica.entity.Paciente;
import com.mycompany.clinica.repository.PacienteRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PacienteControllerTest {

    @Mock
    private PacienteRepository pacienteRepo;

    @InjectMocks
    private PacienteController pacienteController;

    private Paciente crearPaciente(String id, String identificacion) {
        Paciente p = new Paciente();
        p.setId(id);
        p.setNombre("Juan Perez");
        p.setIdentificacion(identificacion);
        p.setPesoKg(20.5);
        p.setEdad(6);
        return p;
    }

    @Test
    void getAll_retornaListaDePacientes() {
        when(pacienteRepo.findAll()).thenReturn(List.of(crearPaciente("1", "100")));

        ResponseEntity<List<Paciente>> respuesta = pacienteController.getAll();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void getById_existente_retorna200() {
        when(pacienteRepo.findById("1")).thenReturn(Optional.of(crearPaciente("1", "100")));

        ResponseEntity<Paciente> respuesta = pacienteController.getById("1");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getIdentificacion()).isEqualTo("100");
    }

    @Test
    void getById_noExiste_retorna404() {
        when(pacienteRepo.findById("99")).thenReturn(Optional.empty());

        ResponseEntity<Paciente> respuesta = pacienteController.getById("99");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void crear_pacienteNuevo_guardaYRetorna200() {
        Paciente paciente = crearPaciente(null, "100");
        when(pacienteRepo.findByIdentificacion("100")).thenReturn(Optional.empty());
        when(pacienteRepo.save(paciente)).thenReturn(paciente);

        ResponseEntity<?> respuesta = pacienteController.crear(paciente);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(pacienteRepo).save(paciente);
    }

    @Test
    void crear_identificacionDuplicada_retorna400YNoGuarda() {
        Paciente paciente = crearPaciente(null, "100");
        when(pacienteRepo.findByIdentificacion("100")).thenReturn(Optional.of(paciente));

        ResponseEntity<?> respuesta = pacienteController.crear(paciente);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(pacienteRepo, never()).save(any(Paciente.class));
    }

    @Test
    void eliminar_existente_borraYRetorna200() {
        when(pacienteRepo.existsById("1")).thenReturn(true);

        ResponseEntity<?> respuesta = pacienteController.eliminar("1");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(pacienteRepo).deleteById("1");
    }

    @Test
    void eliminar_noExiste_retorna404() {
        when(pacienteRepo.existsById("99")).thenReturn(false);

        ResponseEntity<?> respuesta = pacienteController.eliminar("99");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(pacienteRepo, never()).deleteById(any());
    }
}
