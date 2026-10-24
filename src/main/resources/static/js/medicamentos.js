document.addEventListener("DOMContentLoaded", () => {

    const nombreMedicamento = document.getElementById("nombreMedicamento");
    const peso = document.getElementById("peso");
    const dosis = document.getElementById("dosis");
    const unidadDosis = document.getElementById("unidadDosis");
    const concentracion = document.getElementById("concentracion");
    const unidadConcentracion = document.getElementById("unidadConcentracion");

    const btnCalcular = document.getElementById("btnCalcular");
    const btnGuardar = document.getElementById("guardarMedicamento");

    const resultadoMedicamento = document.getElementById("resultadoMedicamento");
    const resultadoPeso = document.getElementById("resultadoPeso");
    const resultadoDosis = document.getElementById("resultadoDosis");
    const resultadoUnidadDosis = document.getElementById("resultadoUnidadDosis");
    const resultadoDosisTotal = document.getElementById("resultadoDosisTotal");
    const unidadDosisTotal = document.getElementById("unidadDosisTotal");
    const resultadoConcentracion = document.getElementById("resultadoConcentracion");
    const resultadoUnidadConcentracion = document.getElementById("resultadoUnidadConcentracion");
    const resultadoVolumen = document.getElementById("resultadoVolumen");

    const mensajeMedicamento = document.getElementById("mensajeMedicamento");

    const tablaMedicamentos = document.getElementById("med-tb");
    const buscador = document.getElementById("med-search");

    let dosisTotalCalculada = 0;
    let volumenCalculado = 0;

    let medicamentosCargados = [];


    // =========================================================
    // CSRF
    // =========================================================

    function obtenerCsrfToken() {

        const inputCsrf = document.querySelector(
            'input[name="_csrf"]'
        );

        return inputCsrf ? inputCsrf.value : null;
    }


    // =========================================================
    // MOSTRAR MENSAJES
    // =========================================================

    function mostrarMensaje(mensaje, tipo = "error") {

        if (!mensajeMedicamento) {
            return;
        }

        mensajeMedicamento.textContent = mensaje;
        mensajeMedicamento.style.display = "block";

        if (tipo === "exito") {
            mensajeMedicamento.classList.add("success");
        } else {
            mensajeMedicamento.classList.remove("success");
        }
    }


    function limpiarMensaje() {

        if (!mensajeMedicamento) {
            return;
        }

        mensajeMedicamento.textContent = "";
        mensajeMedicamento.style.display = "none";
        mensajeMedicamento.classList.remove("success");
    }


    // =========================================================
    // CALCULAR DOSIS
    // =========================================================

    function calcularDosis() {

        limpiarMensaje();

        const nombre = nombreMedicamento.value.trim();

        const pesoPaciente = parseFloat(peso.value);

        const dosisIngresada = parseFloat(dosis.value);

        const concentracionIngresada =
            parseFloat(concentracion.value);


        // VALIDACIONES

        if (nombre === "") {

            mostrarMensaje(
                "Debes ingresar el nombre del medicamento."
            );

            btnGuardar.disabled = true;

            return;
        }


        if (
            isNaN(pesoPaciente) ||
            pesoPaciente <= 0
        ) {

            mostrarMensaje(
                "Debes ingresar un peso válido."
            );

            btnGuardar.disabled = true;

            return;
        }


        if (
            isNaN(dosisIngresada) ||
            dosisIngresada <= 0
        ) {

            mostrarMensaje(
                "Debes ingresar una dosis válida."
            );

            btnGuardar.disabled = true;

            return;
        }


        if (
            isNaN(concentracionIngresada) ||
            concentracionIngresada <= 0
        ) {

            mostrarMensaje(
                "Debes ingresar una concentración válida."
            );

            btnGuardar.disabled = true;

            return;
        }


        // =====================================================
        // CONVERSIÓN DE UNIDADES
        // Todo se lleva primero a mg
        // =====================================================

        let dosisEnMgKg;

        switch (unidadDosis.value) {

            case "mg/kg":
                dosisEnMgKg = dosisIngresada;
                break;

            case "mcg/kg":
                dosisEnMgKg = dosisIngresada / 1000;
                break;

            case "g/kg":
                dosisEnMgKg = dosisIngresada * 1000;
                break;

            default:
                dosisEnMgKg = dosisIngresada;
        }


        // DOSIS TOTAL EN MG

        dosisTotalCalculada =
            pesoPaciente * dosisEnMgKg;


        // =====================================================
        // CONCENTRACIÓN A MG/ML
        // =====================================================

        let concentracionEnMgMl;

        switch (unidadConcentracion.value) {

            case "mg/ml":
                concentracionEnMgMl =
                    concentracionIngresada;
                break;

            case "mcg/ml":
                concentracionEnMgMl =
                    concentracionIngresada / 1000;
                break;

            case "g/ml":
                concentracionEnMgMl =
                    concentracionIngresada * 1000;
                break;

            default:
                concentracionEnMgMl =
                    concentracionIngresada;
        }


        // =====================================================
        // VOLUMEN
        // volumen = dosis total / concentración
        // =====================================================

        volumenCalculado =
            dosisTotalCalculada /
            concentracionEnMgMl;


        // REDONDEAR

        dosisTotalCalculada =
            Math.round(
                dosisTotalCalculada * 100
            ) / 100;

        volumenCalculado =
            Math.round(
                volumenCalculado * 100
            ) / 100;


        // =====================================================
        // MOSTRAR RESULTADOS
        // =====================================================

        resultadoMedicamento.textContent =
            nombre;

        resultadoPeso.textContent =
            pesoPaciente;

        resultadoDosis.textContent =
            dosisIngresada;

        resultadoUnidadDosis.textContent =
            unidadDosis.value;

        resultadoDosisTotal.textContent =
            dosisTotalCalculada;

        unidadDosisTotal.textContent =
            "mg";

        resultadoConcentracion.textContent =
            concentracionIngresada;

        resultadoUnidadConcentracion.textContent =
            unidadConcentracion.value;

        resultadoVolumen.textContent =
            volumenCalculado;


        // HABILITAR GUARDADO

        btnGuardar.disabled = false;

        mostrarMensaje(
            "Cálculo realizado correctamente.",
            "exito"
        );
    }


    // =========================================================
    // GUARDAR MEDICAMENTO EN MONGODB
    // =========================================================

    async function guardarMedicamento() {

        limpiarMensaje();

        const nombre = nombreMedicamento.value.trim();

        if (
            nombre === "" ||
            dosisTotalCalculada <= 0 ||
            volumenCalculado <= 0
        ) {

            mostrarMensaje(
                "Primero debes realizar el cálculo."
            );

            return;
        }


        const medicamento = {

            nombreMedicamento:
                nombre,

            pesoPaciente:
                parseFloat(peso.value),

            dosisPorKg:
                parseFloat(dosis.value),

            unidadDosis:
                unidadDosis.value,

            dosisTotal:
                dosisTotalCalculada,

            concentracion:
                parseFloat(concentracion.value),

            volumenAdministrar:
                volumenCalculado,

            unidadVolumen:
                "ml"
        };


        try {

            const csrfToken =
                obtenerCsrfToken();

            const headers = {
                "Content-Type":
                    "application/json"
            };


            if (csrfToken) {

                headers["X-CSRF-TOKEN"] =
                    csrfToken;
            }


            const response = await fetch(
                "/api/medicamentos",
                {
                    method: "POST",
                    headers: headers,
                    body: JSON.stringify(
                        medicamento
                    )
                }
            );


            if (response.status === 403) {

                mostrarMensaje(
                    "No tienes permisos para guardar medicamentos."
                );

                return;
            }


            if (!response.ok) {

                throw new Error(
                    "No se pudo guardar el medicamento."
                );
            }


            await response.json();


            mostrarMensaje(
                "Medicamento guardado correctamente.",
                "exito"
            );


            btnGuardar.disabled = true;


            await cargarMedicamentos();


        } catch (error) {

            console.error(
                "Error guardando medicamento:",
                error
            );

            mostrarMensaje(
                "Ocurrió un error al guardar el medicamento."
            );
        }
    }


    // =========================================================
    // CARGAR MEDICAMENTOS DE MONGODB
    // =========================================================

    async function cargarMedicamentos() {

        try {

            tablaMedicamentos.innerHTML = `
                <tr class="empty-row">
                    <td colspan="7">
                        Cargando...
                    </td>
                </tr>
            `;


            const response = await fetch(
                "/api/medicamentos"
            );


            if (response.status === 403) {

                tablaMedicamentos.innerHTML = `
                    <tr class="empty-row">
                        <td colspan="7">
                            No tienes permisos para consultar medicamentos.
                        </td>
                    </tr>
                `;

                return;
            }


            if (!response.ok) {

                throw new Error(
                    "No se pudieron cargar los medicamentos."
                );
            }


            medicamentosCargados =
                await response.json();


            mostrarMedicamentos(
                medicamentosCargados
            );


        } catch (error) {

            console.error(
                "Error cargando medicamentos:",
                error
            );


            tablaMedicamentos.innerHTML = `
                <tr class="empty-row">

                    <td colspan="7">

                        No se pudieron cargar
                        los medicamentos.

                    </td>

                </tr>
            `;
        }
    }


    // =========================================================
    // MOSTRAR MEDICAMENTOS EN TABLA
    // =========================================================

    function mostrarMedicamentos(lista) {

        tablaMedicamentos.innerHTML = "";


        if (!lista || lista.length === 0) {

            tablaMedicamentos.innerHTML = `
                <tr class="empty-row">

                    <td colspan="7">

                        No hay medicamentos
                        guardados.

                    </td>

                </tr>
            `;

            return;
        }


        lista.forEach(medicamento => {

            const fila =
                document.createElement("tr");


            fila.innerHTML = `

                <td>
                    ${medicamento.id ?? ""}
                </td>

                <td>
                    ${medicamento.nombreMedicamento ?? ""}
                </td>

                <td>
                    ${medicamento.pesoPaciente ?? ""}
                    kg
                </td>

                <td>
                    ${medicamento.dosisPorKg ?? ""}
                    ${medicamento.unidadDosis ?? ""}
                </td>

                <td>
                    ${medicamento.dosisTotal ?? ""}
                    mg
                </td>

                <td>
                    ${medicamento.volumenAdministrar ?? ""}
                    ${medicamento.unidadVolumen ?? "ml"}
                </td>

                <td>

                    <button
                        type="button"
                        class="btn-sec btn-eliminar-medicamento"
                        data-id="${medicamento.id}"
                    >
                        Eliminar
                    </button>

                </td>

            `;


            tablaMedicamentos.appendChild(
                fila
            );
        });


        agregarEventosEliminar();
    }


    // =========================================================
    // ELIMINAR MEDICAMENTO
    // =========================================================

    function agregarEventosEliminar() {

        const botones =
            document.querySelectorAll(
                ".btn-eliminar-medicamento"
            );


        botones.forEach(boton => {

            boton.addEventListener(
                "click",
                () => {

                    const id =
                        boton.dataset.id;

                    eliminarMedicamento(id);
                }
            );

        });
    }


    async function eliminarMedicamento(id) {

        const confirmar =
            confirm(
                "¿Deseas eliminar este medicamento?"
            );


        if (!confirmar) {
            return;
        }


        try {

            const csrfToken =
                obtenerCsrfToken();

            const headers = {};


            if (csrfToken) {

                headers["X-CSRF-TOKEN"] =
                    csrfToken;
            }


            const response = await fetch(
                `/api/medicamentos/${id}`,
                {
                    method: "DELETE",
                    headers: headers
                }
            );


            if (response.status === 403) {

                mostrarMensaje(
                    "No tienes permisos para eliminar este medicamento."
                );

                return;
            }


            if (response.status === 404) {

                mostrarMensaje(
                    "El medicamento no existe."
                );

                return;
            }


            if (!response.ok) {

                throw new Error(
                    "No se pudo eliminar."
                );
            }


            mostrarMensaje(
                "Medicamento eliminado correctamente.",
                "exito"
            );


            await cargarMedicamentos();


        } catch (error) {

            console.error(
                "Error eliminando medicamento:",
                error
            );


            mostrarMensaje(
                "Ocurrió un error al eliminar el medicamento."
            );
        }
    }


    // =========================================================
    // BUSCADOR
    // =========================================================

    if (buscador) {

        buscador.addEventListener(
            "input",
            () => {

                const texto =
                    buscador.value
                        .trim()
                        .toLowerCase();


                const filtrados =
                    medicamentosCargados.filter(
                        medicamento => {

                            const nombre =
                                medicamento
                                    .nombreMedicamento
                                    ?.toLowerCase()
                                || "";

                            return nombre.includes(
                                texto
                            );
                        }
                    );


                mostrarMedicamentos(
                    filtrados
                );
            }
        );
    }


    // =========================================================
    // EVENTOS PRINCIPALES
    // =========================================================

    btnCalcular.addEventListener(
        "click",
        calcularDosis
    );


    btnGuardar.addEventListener(
        "click",
        guardarMedicamento
    );


    // =========================================================
    // CARGAR AL ABRIR LA PÁGINA
    // =========================================================

    cargarMedicamentos();

});