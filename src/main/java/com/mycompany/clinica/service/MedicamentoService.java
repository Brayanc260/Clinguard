
package com.mycompany.clinica.service;

import com.mycompany.clinica.entity.Medicamento;
import com.mycompany.clinica.repository.MedicamentoRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;
    private final NotificacionService notificacionService;

    public MedicamentoService(
            MedicamentoRepository medicamentoRepository,
            NotificacionService notificacionService) {

        this.medicamentoRepository = medicamentoRepository;
        this.notificacionService = notificacionService;
    }

    // =========================================
    // LISTAR TODOS
    // =========================================
    public List<Medicamento> listarTodos() {
        return medicamentoRepository.findAll();
    }

    // =========================================
    // GUARDAR MEDICAMENTO
    // =========================================
    public Medicamento guardar(Medicamento medicamento) {

        if (medicamento.getEstado() == null
                || medicamento.getEstado().isBlank()) {
            medicamento.setEstado("ACTIVO");
        }

        if (medicamento.getStock() == null) {
            medicamento.setStock(0);
        }

        if (medicamento.getStock() < 0) {
            throw new IllegalArgumentException(
                "El stock no puede ser negativo."
            );
        }

        // Detectar stock anterior si se guarda un ID existente.
        Integer stockAnterior = null;

        if (medicamento.getId() != null
                && !medicamento.getId().isBlank()) {

            stockAnterior = medicamentoRepository
                    .findById(medicamento.getId())
                    .map(Medicamento::getStock)
                    .orElse(null);
        }

        // Primero guardar en MongoDB.
        Medicamento guardado =
                medicamentoRepository.save(medicamento);

        // Notificar despuÃ©s de guardar.
        enviarAlertaStockBajo(
                guardado,
                stockAnterior
        );

        return guardado;
    }

    // =========================================
    // BUSCAR POR ID
    // =========================================
    public Optional<Medicamento> buscarPorId(String id) {
        return medicamentoRepository.findById(id);
    }

    // =========================================
    // BUSCAR POR NOMBRE
    // =========================================
    public List<Medicamento> buscarPorNombre(String nombre) {
        return medicamentoRepository
                .findByNombreContainingIgnoreCase(nombre);
    }

    // =========================================
    // BUSCAR POR PRINCIPIO ACTIVO
    // =========================================
    public List<Medicamento> buscarPorPrincipioActivo(
            String principioActivo) {

        return medicamentoRepository
                .findByPrincipioActivoContainingIgnoreCase(
                        principioActivo
                );
    }

    // =========================================
    // LISTAR ACTIVOS
    // =========================================
    public List<Medicamento> listarActivos() {
        return medicamentoRepository.findByEstado("ACTIVO");
    }

    // =========================================
    // ACTUALIZAR MEDICAMENTO
    // =========================================
    public Medicamento actualizar(
            String id,
            Medicamento medicamentoActualizado) {

        Medicamento medicamento = medicamentoRepository
                .findById(id)
                .orElseThrow(
                    () -> new RuntimeException(
                        "Medicamento no encontrado"
                    )
                );

        // Guardar stock anterior para comparaciÃ³n.
        Integer stockAnterior = medicamento.getStock();

        Integer stockNuevo = medicamentoActualizado.getStock();

        if (stockNuevo == null || stockNuevo < 0) {
            throw new IllegalArgumentException(
                "Debes ingresar un stock vÃ¡lido."
            );
        }

        medicamento.setNombre(
                medicamentoActualizado.getNombre()
        );

        medicamento.setPrincipioActivo(
                medicamentoActualizado.getPrincipioActivo()
        );

        medicamento.setPresentacion(
                medicamentoActualizado.getPresentacion()
        );

        medicamento.setCantidadMedicamento(
                medicamentoActualizado.getCantidadMedicamento()
        );

        medicamento.setUnidadMedicamento(
                medicamentoActualizado.getUnidadMedicamento()
        );

        medicamento.setCantidadVehiculo(
                medicamentoActualizado.getCantidadVehiculo()
        );

        medicamento.setUnidadVehiculo(
                medicamentoActualizado.getUnidadVehiculo()
        );

        medicamento.setViaAdministracion(
                medicamentoActualizado.getViaAdministracion()
        );

        medicamento.setStock(stockNuevo);

        medicamento.setEstado(
                medicamentoActualizado.getEstado()
        );

        // Guardar cambios.
        Medicamento guardado =
                medicamentoRepository.save(medicamento);

        // Comprobar si el stock cruzÃ³ el umbral.
        enviarAlertaStockBajo(
                guardado,
                stockAnterior
        );

        return guardado;
    }

    // =========================================
    // ALERTA AUTOMÃTICA DE STOCK BAJO
    // =========================================
    private void enviarAlertaStockBajo(
            Medicamento medicamento,
            Integer stockAnterior) {

        if (medicamento == null
                || medicamento.getStock() == null) {
            return;
        }

        try {
            notificacionService.notificarStockBajo(
                    medicamento.getId(),
                    medicamento.getNombre(),
                    stockAnterior,
                    medicamento.getStock()
            );

        } catch (Exception e) {

            // El medicamento permanece guardado
            // aunque falle la notificaciÃ³n.
            System.err.println(
                "Error enviando alerta de stock: "
                + e.getMessage()
            );
        }
    }

    // =========================================
    // ELIMINAR MEDICAMENTO
    // =========================================
    public void eliminar(String id) {
        medicamentoRepository.deleteById(id);
    }
}
