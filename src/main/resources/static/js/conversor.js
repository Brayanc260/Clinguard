document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    // ============================================
    // CLINGUARD - CONVERSOR MÉDICO PROFESIONAL
    // ============================================

    const opciones = document.querySelectorAll(".copt");

    const inputValor = document.getElementById("conv-val");
    const inputPeso = document.getElementById("pesoPaciente");
    const inputDosis = document.getElementById("dosisDisponible");
    const inputVolumen = document.getElementById("volumenDisponible");

    const botonCalcular = document.querySelector(".conversion-submit");
    const resultadoValor = document.getElementById("res-val");
    const resultadoUnidad = document.getElementById("res-unit");

    if (!inputValor || !botonCalcular || !resultadoValor || !resultadoUnidad) {
        return;
    }

    // ============================================
    // ESTADO DEL CONVERSOR
    // ============================================
    let operacionSeleccionada = document.querySelector(".copt.sel")?.dataset.op || "g-mg";

    // ============================================
    // CAMPOS DINÁMICOS
    // ============================================
    const campos = [inputValor, inputPeso, inputDosis, inputVolumen];

    function obtenerGrupo(input) {
        return input?.closest(".form-group") || null;
    }

    const etiquetaValor = obtenerGrupo(inputValor)?.querySelector("label");
    const etiquetaPeso = obtenerGrupo(inputPeso)?.querySelector("label");
    const etiquetaDosis = obtenerGrupo(inputDosis)?.querySelector("label");
    const etiquetaVolumen = obtenerGrupo(inputVolumen)?.querySelector("label");

    // ============================================
    // SELECCIÓN DE OPERACIONES (BOTONES .copt)
    // ============================================
    opciones.forEach(opcion => {
        opcion.addEventListener("click", () => {
            opciones.forEach(item => item.classList.remove("sel"));
            opcion.classList.add("sel");
            operacionSeleccionada = opcion.dataset.op;

            actualizarCampos();
            calcularConversion();
        });
    });

    function mostrarCampo(input, mostrar) {
        if (!input) return;
        const grupo = obtenerGrupo(input);
        if (grupo) {
            grupo.hidden = !mostrar;
            grupo.style.display = mostrar ? "" : "none";
        }
        input.disabled = !mostrar;
        input.required = mostrar;
    }

    function actualizarCampos() {
        const op = operacionSeleccionada;
        const usaPeso = ["dosis-mg-kg", "dosis-ug-kg", "dosis-total"].includes(op);
        const usaConcentracion = op === "dosis-volumen";

        mostrarCampo(inputValor, true);
        mostrarCampo(inputPeso, usaPeso);
        mostrarCampo(inputDosis, usaConcentracion);
        mostrarCampo(inputVolumen, usaConcentracion);

        if (etiquetaValor) {
            const nombres = {
                "dosis-mg-kg": "Dosis total a validar (mg)",
                "dosis-ug-kg": "Dosis total (µg)",
                "dosis-total": "Dosis indicada por kg (mg/kg)",
                "dosis-volumen": "Dosis requerida (mg)"
            };
            etiquetaValor.textContent = nombres[op] || "Valor a convertir";
        }
    }

    actualizarCampos();

    // ============================================
    // OBTENER VALORES
    // ============================================
    function obtenerNumero(input) {
        if (!input || input.disabled) return null;
        const texto = input.value.trim();
        if (texto === "") return null;
        const numero = Number(texto);
        return Number.isFinite(numero) ? numero : null;
    }

    function esPositivo(numero) {
        return numero !== null && Number.isFinite(numero) && numero > 0;
    }

    function formatearNumero(numero) {
        if (Number.isInteger(numero)) return numero.toString();
        return parseFloat(numero.toFixed(4)).toString();
    }

    function mostrarResultado(resultado, unidad) {
        resultadoValor.textContent = formatearNumero(resultado);
        resultadoUnidad.textContent = unidad;
    }

    function mostrarError(mensaje) {
        resultadoValor.textContent = "—";
        resultadoUnidad.textContent = mensaje;
    }

    // ============================================
    // CALCULAR CONVERSIÓN
    // ============================================
    function calcularConversion() {
        if (!operacionSeleccionada) {
            mostrarError("Selecciona una conversión.");
            return;
        }

        const valor = obtenerNumero(inputValor);
        const peso = obtenerNumero(inputPeso);
        const dosisDisponible = obtenerNumero(inputDosis);
        const volumenDisponible = obtenerNumero(inputVolumen);

        if (valor === null || valor < 0) {
            resultadoValor.textContent = "—";
            resultadoUnidad.textContent = "Ingresa un valor numérico para calcular.";
            evaluarSemaforo();
            return;
        }

        let resultado;
        let unidad = "";

        switch (operacionSeleccionada) {
            // Masa
            case "g-mg":
                resultado = valor * 1000;
                unidad = "mg (Miligramos)";
                break;
            case "mg-g":
                resultado = valor / 1000;
                unidad = "g (Gramos)";
                break;
            case "ug-mg":
                resultado = valor / 1000;
                unidad = "mg (Miligramos)";
                break;
            case "kg-g":
                resultado = valor * 1000;
                unidad = "g (Gramos)";
                break;
            case "g-kg":
                resultado = valor / 1000;
                unidad = "kg (Kilogramos)";
                break;
            case "mg-kg":
                resultado = valor / 1000000;
                unidad = "kg (Kilogramos)";
                break;

            // Volumen
            case "ml-l":
                resultado = valor / 1000;
                unidad = "L (Litros)";
                break;
            case "l-ml":
                resultado = valor * 1000;
                unidad = "mL (Mililitros)";
                break;

            // Dosis y peso
            case "dosis-mg-kg":
                if (!esPositivo(peso)) {
                    mostrarError("Ingresa el peso del paciente (kg).");
                    evaluarSemaforo();
                    return;
                }
                resultado = valor / peso;
                unidad = "mg/kg (Dosis relativa por peso)";
                break;

            case "dosis-ug-kg":
                if (!esPositivo(peso)) {
                    mostrarError("Ingresa el peso del paciente (kg).");
                    evaluarSemaforo();
                    return;
                }
                resultado = valor / peso;
                unidad = "µg/kg";
                break;

            case "dosis-total":
                if (!esPositivo(peso)) {
                    mostrarError("Ingresa el peso del paciente (kg).");
                    evaluarSemaforo();
                    return;
                }
                resultado = valor * peso;
                unidad = "mg (Dosis total absoluta)";
                break;

            case "dosis-volumen":
                if (!esPositivo(dosisDisponible)) {
                    mostrarError("La dosis disponible debe ser mayor a cero.");
                    return;
                }
                if (!esPositivo(volumenDisponible)) {
                    mostrarError("El volumen disponible debe ser mayor a cero.");
                    return;
                }
                resultado = (valor * volumenDisponible) / dosisDisponible;
                unidad = "mL a administrar";
                break;

            default:
                mostrarError("Operación no disponible.");
                return;
        }

        mostrarResultado(resultado, unidad);
        evaluarSemaforo();
    }

    // Auto-cálculo en tiempo real al escribir
    campos.forEach(input => {
        if (!input) return;
        input.addEventListener("input", () => {
            calcularConversion();
        });
    });

    botonCalcular.addEventListener("click", event => {
        event.preventDefault();
        calcularConversion();
    });

    // ============================================
    // GESTIÓN DE PACIENTES Y MEDICAMENTOS
    // ============================================
    const selectPaciente = document.getElementById("conv-paciente");
    const buscarPaciente = document.getElementById("conv-buscar-paciente");
    const selectMedicamento = document.getElementById("conv-medicamento");
    const buscarMedicamento = document.getElementById("conv-buscar-medicamento");

    // Filtrar paciente por cédula o nombre
    if (selectPaciente && buscarPaciente) {
        const opcionesPacientes = Array.from(selectPaciente.options).slice(1).map(opt => ({
            id: opt.value,
            text: opt.textContent,
            peso: opt.getAttribute("data-peso"),
            search: opt.textContent.toLowerCase()
        }));

        buscarPaciente.addEventListener("input", () => {
            const term = buscarPaciente.value.trim().toLowerCase();
            selectPaciente.innerHTML = '<option value="">Seleccionar paciente</option>';
            const filtrados = opcionesPacientes.filter(p => p.search.includes(term));
            filtrados.forEach(p => {
                const opt = document.createElement("option");
                opt.value = p.id;
                opt.textContent = p.text;
                if (p.peso) opt.setAttribute("data-peso", p.peso);
                selectPaciente.appendChild(opt);
            });
            if (filtrados.length === 1) {
                selectPaciente.value = filtrados[0].id;
                selectPaciente.dispatchEvent(new Event("change"));
            }
        });

        // Al seleccionar un paciente, autocompletar su peso en el input
        selectPaciente.addEventListener("change", () => {
            const opt = selectPaciente.options[selectPaciente.selectedIndex];
            const peso = opt?.getAttribute("data-peso");
            if (peso && parseFloat(peso) > 0 && inputPeso) {
                inputPeso.value = parseFloat(peso);
                calcularConversion();
            }
        });
    }

    // Filtrar medicamento
    if (selectMedicamento && buscarMedicamento) {
        const opcionesMeds = Array.from(selectMedicamento.options).slice(1).map(opt => ({
            id: opt.value,
            text: opt.textContent,
            min: opt.getAttribute("data-min"),
            max: opt.getAttribute("data-max"),
            search: opt.textContent.toLowerCase()
        }));

        buscarMedicamento.addEventListener("input", () => {
            const term = buscarMedicamento.value.trim().toLowerCase();
            selectMedicamento.innerHTML = '<option value="">Seleccionar medicamento del vademécum</option>';
            const filtrados = opcionesMeds.filter(m => m.search.includes(term));
            filtrados.forEach(m => {
                const opt = document.createElement("option");
                opt.value = m.id;
                opt.textContent = m.text;
                if (m.min) opt.setAttribute("data-min", m.min);
                if (m.max) opt.setAttribute("data-max", m.max);
                selectMedicamento.appendChild(opt);
            });
            if (filtrados.length === 1) {
                selectMedicamento.value = filtrados[0].id;
                selectMedicamento.dispatchEvent(new Event("change"));
            }
        });

        selectMedicamento.addEventListener("change", () => {
            evaluarSemaforo();
        });
    }

    // ============================================
    // EVALUACIÓN DEL SEMÁFORO CLÍNICO
    // ============================================
    const semRed = document.getElementById("sem-red");
    const semYellow = document.getElementById("sem-yellow");
    const semGreen = document.getElementById("sem-green");
    const semBlue = document.getElementById("sem-blue");
    const semObs = document.getElementById("semaforo-observacion");

    function resetSemaforo() {
        if (!semRed) return;
        semRed.className = "sem-circle";
        semYellow.className = "sem-circle";
        semGreen.className = "sem-circle";
        semBlue.className = "sem-circle";
        if (semObs) {
            semObs.style.display = "none";
            semObs.textContent = "";
            semObs.className = "semaforo-obs";
        }
    }

    function evaluarSemaforo() {
        if (!selectMedicamento || !selectMedicamento.value) {
            resetSemaforo();
            return;
        }

        const peso = parseFloat(inputPeso ? inputPeso.value : 0) || 0;
        const dosisIngresada = parseFloat(inputValor ? inputValor.value : 0) || 0;
        const selectedOpt = selectMedicamento.options[selectMedicamento.selectedIndex];
        const minMgKg = parseFloat(selectedOpt ? selectedOpt.getAttribute("data-min") : 0) || 0;
        const maxMgKg = parseFloat(selectedOpt ? selectedOpt.getAttribute("data-max") : 0) || 0;

        if (peso <= 0 || minMgKg <= 0 || maxMgKg <= 0) {
            resetSemaforo();
            return;
        }

        const dosisMinTotal = minMgKg * peso;
        const dosisMaxTotal = maxMgKg * peso;
        const dosisSugerida = (dosisMinTotal + dosisMaxTotal) / 2;

        resetSemaforo();

        if (dosisIngresada <= 0) {
            semBlue.className = "sem-circle active-blue";
            if (semObs) {
                semObs.className = "semaforo-obs suggest";
                semObs.textContent = "Dosis sugerida: " + dosisSugerida.toFixed(1) + " mg (Rango seguro: " + dosisMinTotal.toFixed(1) + " mg - " + dosisMaxTotal.toFixed(1) + " mg)";
            }
        } else if (dosisIngresada < dosisMinTotal) {
            semYellow.className = "sem-circle active-yellow";
            if (semObs) {
                semObs.className = "semaforo-obs warn";
                semObs.textContent = "ALERTA (Infradosis): La dosis de " + dosisIngresada + " mg está por debajo del mínimo requerido (" + dosisMinTotal.toFixed(1) + " mg). Eficacia comprometida.";
            }
        } else if (dosisIngresada > dosisMaxTotal) {
            semRed.className = "sem-circle active-red";
            if (semObs) {
                semObs.className = "semaforo-obs danger";
                semObs.textContent = "PELIGRO (Sobredosis): La dosis de " + dosisIngresada + " mg supera el límite máximo seguro (" + dosisMaxTotal.toFixed(1) + " mg). Riesgo de toxicidad.";
            }
        } else {
            semGreen.className = "sem-circle active-green";
            if (semObs) {
                semObs.className = "semaforo-obs safe";
                semObs.textContent = "DOSIS SEGURA: " + dosisIngresada + " mg está dentro del rango terapéutico óptimo (" + dosisMinTotal.toFixed(1) + " - " + dosisMaxTotal.toFixed(1) + " mg).";
            }
        }
    }
});