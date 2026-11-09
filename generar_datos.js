const fs = require('fs');

// Genera fechas en formato simple YYYY-MM-DD (texto)
function fechaAleatoria() {
    const hoy = new Date();
    hoy.setMonth(hoy.getMonth() - Math.floor(Math.random() * 6));
    return hoy.toISOString().split('T')[0];
}

// Genera horas en formato HH:mm
function horaAleatoria() {
    const horas = [8, 9, 10, 11, 14, 15, 16, 17];
    const h = horas[Math.floor(Math.random() * horas.length)];
    return `${h.toString().padStart(2, '0')}:00`;
}

// --- PACIENTES ---
const nombres = ["Carlos", "María", "Juan", "Ana", "Luis", "Laura", "Pedro", "Sofía", "Diego", "Valentina", "Andrés", "Camila", "Jorge", "Daniela", "Felipe", "Isabella", "Ricardo", "Gabriela", "Javier", "Mariana"];
const apellidos = ["Gómez", "Rodríguez", "Martínez", "Pérez", "López", "Silva", "Castro", "Rojas", "Sánchez", "Fernández", "Torres", "Ramírez", "Flores", "Vargas", "Ríos", "Mendoza", "Cruz", "Ortiz", "Herrera", "Cárdenas"];

let pacientes = [];
for (let i = 1; i <= 50; i++) {
    const esNiño = Math.random() < 0.2;
    pacientes.push({
        _id: `pac_${i}`, 
        tipoDocumento: Math.random() > 0.5 ? "CC" : "TI",
        numeroDocumento: (1000000000 + Math.floor(Math.random() * 900000000)).toString(),
        nombres: nombres[Math.floor(Math.random() * nombres.length)],
        apellidos: `${apellidos[Math.floor(Math.random() * apellidos.length)]} ${apellidos[Math.floor(Math.random() * apellidos.length)]}`,
        fechaNacimiento: esNiño ? `2015-0${Math.floor(Math.random()*9)+1}-1${Math.floor(Math.random()*9)}` : `19${Math.floor(Math.random()*30)+60}-0${Math.floor(Math.random()*9)+1}-1${Math.floor(Math.random()*9)}`,
        sexo: Math.random() > 0.5 ? "Masculino" : "Femenino",
        tipoSangre: ["O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"][Math.floor(Math.random() * 8)],
        eps: ["SURA", "Sanitas", "Nueva EPS", "Compensar", "Famisanar", "Coomeva"][Math.floor(Math.random() * 6)],
        peso: esNiño ? (15 + Math.random() * 20).toFixed(1) : (50 + Math.random() * 40).toFixed(1),
        estatura: esNiño ? Math.floor(100 + Math.random() * 40) : Math.floor(150 + Math.random() * 30),
        fechaRegistro: fechaAleatoria(),
        activo: true
    });
}
fs.writeFileSync('pacientes.json', JSON.stringify(pacientes, null, 2));

// --- CITAS ---
const estados = ["PROGRAMADA", "CONFIRMADA", "ATENDIDA", "CANCELADA"];
const motivos = ["Control de rutina", "Dolor abdominal", "Cefalea", "Fiebre", "Chequeo general", "Dolor de garganta"];

let citas = [];
for (let i = 0; i < 50; i++) {
    citas.push({
        pacienteId: `pac_${Math.floor(Math.random() * 50) + 1}`,
        medicoUsername: "medico",
        fecha: fechaAleatoria(),
        hora: horaAleatoria(),
        motivo: motivos[Math.floor(Math.random() * motivos.length)],
        observaciones: "Paciente reporta sintomatología leve.",
        estado: estados[Math.floor(Math.random() * estados.length)]
    });
}
fs.writeFileSync('citas.json', JSON.stringify(citas, null, 2));

// --- HISTORIALES ---
let historiales = [];
for (let i = 0; i < 50; i++) {
    historiales.push({
        pacienteId: `pac_${Math.floor(Math.random() * 50) + 1}`,
        medicoUsername: "medico",
        fecha: fechaAleatoria(),
        motivoConsulta: motivos[Math.floor(Math.random() * motivos.length)],
        diagnostico: "Paciente estable",
        tratamiento: "Reposo y hidratación",
        observaciones: "Sin novedades"
    });
}
fs.writeFileSync('historiales.json', JSON.stringify(historiales, null, 2));
console.log("✅ Archivos generados correctamente (fechas de texto y horas válidas).");