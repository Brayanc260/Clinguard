
(() => {
    "use strict";

    // ==========================================
    // CLINGUARD - REPORTES
    // ==========================================

    const $ = id => document.getElementById(id);

    const tipo = $("tipoReporte");
    const desde = $("fechaDesde");
    const hasta = $("fechaHasta");

    const area = $("resultadoReporte");
    const cuerpo = $("resultadoReporteCuerpo");
    const estado = $("estadoReporte");
    const titulo = $("resultadoReporteTitulo");

    let datos = null;
    let filtrosConsultados = null;

    if (!tipo || !desde || !hasta ||
        !area || !cuerpo || !estado || !titulo) {
        console.error("Faltan elementos del módulo Reportes.");
        return;
    }

    // ==========================================
    // NOMBRES DE REPORTES
    // ==========================================

    const nombresReportes = {
        general: "General",
        pacientes: "Pacientes",
        citas: "Citas",
        historiales: "Historiales médicos",
        conversiones: "Conversiones Médicas"
    };

    // ==========================================
    // MENSAJES
    // ==========================================

    function aviso(mensaje, error = false) {
        estado.textContent = mensaje;
        estado.style.color = error ? "#ff8e9a" : "";
    }

    // ==========================================
    // OBTENER FILTROS
    // ==========================================

    function filtros() {

        const q = new URLSearchParams({
            tipo: tipo.value
        });

        if (desde.value) {
            q.set("desde", desde.value);
        }

        if (hasta.value) {
            q.set("hasta", hasta.value);
        }

        return q;
    }

    // ==========================================
    // VALIDAR FECHAS
    // ==========================================

    function validarFechas() {

        if (desde.value &&
            hasta.value &&
            desde.value > hasta.value) {

            aviso(
                "La fecha Desde debe ser anterior o igual a Hasta.",
                true
            );

            return false;
        }

        return true;
    }

    // ==========================================
    // CREAR CELDAS SEGURAS
    // ==========================================

    function crearCelda(tr, valor, etiqueta = "td") {

        const celda = document.createElement(etiqueta);

        celda.textContent = String(valor ?? "");

        tr.appendChild(celda);
    }

    // ==========================================
    // DIBUJAR REPORTE
    // ==========================================

    function dibujar(data) {

        datos = data;

        titulo.textContent =
            "Resultado: " +
            (nombresReportes[data.tipo] || data.tipo);

        cuerpo.replaceChildren();

        const filas = Array.isArray(data.filas)
            ? data.filas
            : [];

        if (filas.length === 0) {

            const mensaje = document.createElement("p");

            mensaje.className = "muted-text-sm";

            mensaje.textContent =
                "No hay registros para los filtros seleccionados.";

            cuerpo.appendChild(mensaje);

        } else {

            const tabla = document.createElement("table");
            const thead = document.createElement("thead");
            const encabezado = document.createElement("tr");

            const columnas = Object.keys(filas[0]);

            columnas.forEach(columna => {
                crearCelda(encabezado, columna, "th");
            });

            thead.appendChild(encabezado);
            tabla.appendChild(thead);

            const tbody = document.createElement("tbody");

            filas.forEach(fila => {

                const tr = document.createElement("tr");

                columnas.forEach(columna => {
                    crearCelda(tr, fila[columna]);
                });

                tbody.appendChild(tr);
            });

            tabla.appendChild(tbody);

            const contenedor = document.createElement("div");

            contenedor.className = "table-container";

            contenedor.appendChild(tabla);

            cuerpo.appendChild(contenedor);
        }

        area.hidden = false;

        aviso(
            "Registros consultados: " +
            filas.length +
            ". Los filtros de fecha se aplican a los registros."
        );

        area.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    }

    // ==========================================
    // CONSULTAR DATOS EN MONGODB
    // ==========================================

    async function consultar() {

        if (!validarFechas()) {
            return false;
        }

        aviso("Consultando datos de MongoDB...");

        const consultaActual = filtros().toString();

        try {

            const respuesta = await fetch(
                "/reportes/datos?" + consultaActual,
                {
                    method: "GET",
                    credentials: "same-origin",
                    headers: {
                        "Accept": "application/json"
                    }
                }
            );

            const data = await respuesta.json()
                .catch(() => ({}));

            if (!respuesta.ok) {

                throw new Error(
                    data.error ||
                    `Error HTTP ${respuesta.status}`
                );
            }

            if (!data || !Array.isArray(data.filas)) {

                throw new Error(
                    "El servidor devolvió un reporte no válido."
                );
            }

            dibujar(data);

            filtrosConsultados = consultaActual;

            return true;

        } catch (error) {

            // Evita imprimir o exportar datos anteriores
            // cuando falla una nueva consulta.
            datos = null;
            filtrosConsultados = null;
            area.hidden = true;
            cuerpo.replaceChildren();

            aviso(
                error.message ||
                "No se pudo cargar el reporte.",
                true
            );

            return false;
        }
    }

    // ==========================================
    // CAMBIAR TIPO DE REPORTE
    // ==========================================

    async function cambiar(tipoSeleccionado) {

        const opcionExiste = Array.from(tipo.options)
            .some(op => op.value === tipoSeleccionado);

        if (!opcionExiste) {

            aviso(
                "Este tipo de reporte no está disponible.",
                true
            );

            return;
        }

        tipo.value = tipoSeleccionado;

        await consultar();
    }

    // ==========================================
    // ASOCIAR BOTONES
    // ==========================================

    function asociarBoton(id, accion) {

        const boton = $(id);

        if (boton) {
            boton.addEventListener("click", accion);
        }
    }

    // ==========================================
    // BOTONES PRINCIPALES
    // ==========================================

    asociarBoton("btnAplicarReporte", consultar);

    asociarBoton("btnGenerarReporte", consultar);

    // ==========================================
    // REPORTES EXISTENTES
    // ==========================================

    asociarBoton(
        "btnReportePacientes",
        () => cambiar("pacientes")
    );

    asociarBoton(
        "btnReporteCitas",
        () => cambiar("citas")
    );

    asociarBoton(
        "btnReporteHistoriales",
        () => cambiar("historiales")
    );

    // ==========================================
    // NUEVO REPORTE: CONVERSIONES MÉDICAS
    // ==========================================

    asociarBoton(
        "btnReporteConversiones",
        () => cambiar("conversiones")
    );

    // ==========================================
    // LIMPIAR FILTROS
    // ==========================================

    asociarBoton(
        "btnLimpiarReporte",
        async () => {

            tipo.value = "general";
            desde.value = "";
            hasta.value = "";

            await consultar();
        }
    );

    // ==========================================
    // FORMATO CSV
    // ==========================================

    function csvCampo(valor) {

        return '"' +
            String(valor ?? "")
                .replaceAll('"', '""') +
            '"';
    }

    // ==========================================
    // PROTECCIÓN DE CSV
    // ==========================================

    function csvSeguro(valor) {

        const texto = String(valor ?? "");

        // Evita que Excel interprete datos
        // no confiables como fórmulas.
        if (/^[\s]*[=+@-]/.test(texto)) {
            return "'" + texto;
        }

        return texto;
    }

    // ==========================================
    // EXPORTAR A EXCEL (CSV)
    // ==========================================

    async function exportarExcel() {

        if (!(await consultar())) {
            return;
        }

        const filas = datos.filas || [];

        if (!filas.length) {

            aviso(
                "No hay información para exportar.",
                true
            );

            return;
        }

        const columnas = Object.keys(filas[0]);

        const lineas = [
            columnas.map(csvCampo).join(";"),

            ...filas.map(fila =>
                columnas.map(columna =>
                    csvCampo(
                        csvSeguro(fila[columna])
                    )
                ).join(";")
            )
        ];

        const blob = new Blob(
            [
                "\ufeff" + lineas.join("\r\n")
            ],
            {
                type: "text/csv;charset=utf-8;"
            }
        );

        const url = URL.createObjectURL(blob);

        const enlace = document.createElement("a");

        enlace.href = url;

        enlace.download =
            `clinguard_${datos.tipo}_` +
            `${new Date().toISOString().slice(0, 10)}.csv`;

        document.body.appendChild(enlace);

        enlace.click();
        enlace.remove();

        // Liberar el enlace temporal después
        // de que el navegador procese la descarga.
        setTimeout(() => {
            URL.revokeObjectURL(url);
        }, 1000);

        aviso(
            "Se descargó un CSV compatible con Excel."
        );
    }

    asociarBoton(
        "btnExportarExcel",
        exportarExcel
    );

    // ==========================================
    // COMPROBAR VIGENCIA DEL REPORTE
    // ==========================================

    async function prepararExportacion() {

        const consultaActual = filtros().toString();

        if (!datos ||
            filtrosConsultados !== consultaActual) {

            return await consultar();
        }

        return true;
    }

    // ==========================================
    // IMPRIMIR
    // ==========================================

    async function imprimir() {

        if (!(await prepararExportacion())) {
            return;
        }

        window.print();
    }

    asociarBoton(
        "btnImprimir",
        imprimir
    );

    // ==========================================
    // EXPORTAR PDF
    // ==========================================

    asociarBoton(
        "btnExportarPDF",
        async () => {

            if (!(await prepararExportacion())) {
                return;
            }

            aviso(
                "En la ventana de impresión selecciona «Guardar como PDF»."
            );

            window.print();
        }
    );

    // ==========================================
    // INVALIDAR DATOS AL CAMBIAR FILTROS
    // ==========================================

    function invalidarReporte() {

        datos = null;
        filtrosConsultados = null;

        area.hidden = true;
        cuerpo.replaceChildren();

        aviso(
            "Los filtros cambiaron. Genera el reporte para ver los nuevos datos."
        );
    }

    tipo.addEventListener(
        "change",
        invalidarReporte
    );

    desde.addEventListener(
        "change",
        invalidarReporte
    );

    hasta.addEventListener(
        "change",
        invalidarReporte
    );

})();
