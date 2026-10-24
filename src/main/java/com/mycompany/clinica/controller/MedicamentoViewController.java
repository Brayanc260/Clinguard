package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Medicamento;
import com.mycompany.clinica.service.MedicamentoService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/medicamentos")
public class MedicamentoViewController {

    private final MedicamentoService medicamentoService;

    public MedicamentoViewController(
            MedicamentoService medicamentoService) {

        this.medicamentoService = medicamentoService;
    }


    // =========================================
    // MOSTRAR PÃƒÆ’Ã‚ÂGINA DE MEDICAMENTOS
    // =========================================
    @GetMapping
    public String mostrarMedicamentos(
            Model model) {

        model.addAttribute(
                "medicamentos",
                medicamentoService.listarTodos()
        );

        model.addAttribute(
                "medicamento",
                new Medicamento()
        );

        return "medicamentos";
    }


    // =========================================
    // GUARDAR MEDICAMENTO
    // =========================================
    @PostMapping("/guardar")
    public String guardarMedicamento(
            @ModelAttribute Medicamento medicamento,
            RedirectAttributes redirectAttributes) {

        try {

            medicamentoService.guardar(medicamento);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Medicamento guardado correctamente."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo guardar el medicamento."
            );
        }

        return "redirect:/medicamentos";
    }


    // =========================================
    // EDITAR MEDICAMENTO
    // =========================================
    @PostMapping("/editar/{id}")
    public String editarMedicamento(
            @PathVariable String id,
            @ModelAttribute Medicamento medicamento,
            RedirectAttributes redirectAttributes) {

        try {

            medicamentoService.actualizar(
                    id,
                    medicamento
            );

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Medicamento actualizado correctamente."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo actualizar el medicamento."
            );
        }

        return "redirect:/medicamentos";
    }


    // =========================================
    // ELIMINAR MEDICAMENTO
    // =========================================
    @PostMapping("/eliminar/{id}")
    public String eliminarMedicamento(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        try {

            medicamentoService.eliminar(id);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Medicamento eliminado correctamente."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo eliminar el medicamento."
            );
        }

        return "redirect:/medicamentos";
    }


    // =========================================
    // BUSCAR MEDICAMENTO
    // =========================================
    @GetMapping("/buscar")
    public String buscarMedicamento(
            @RequestParam(required = false)
            String nombre,
            Model model) {

        if (nombre == null || nombre.isBlank()) {

            model.addAttribute(
                    "medicamentos",
                    medicamentoService.listarTodos()
            );

        } else {

            model.addAttribute(
                    "medicamentos",
                    medicamentoService
                            .buscarPorNombre(nombre)
            );
        }

        model.addAttribute(
                "medicamento",
                new Medicamento()
        );

        return "medicamentos";
    }
}