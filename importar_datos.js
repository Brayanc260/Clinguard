const { MongoClient } = require('mongodb');
const fs = require('fs');

// La URI se lee de la variable de entorno MONGO_URI (no va escrita en el código)
const uri = process.env.MONGO_URI;
if (!uri) {
    console.error("Falta la variable de entorno MONGO_URI");
    process.exit(1);
}

// Convierte "YYYY-MM-DD" a fecha; devuelve null si no es válida.
// Se usa mediodía UTC para que la fecha no cambie de día por la zona horaria.
function aFecha(valor) {
    const f = new Date(valor + "T12:00:00Z");
    return isNaN(f) ? null : f;
}

async function importar() {
    const client = new MongoClient(uri);
    try {
        await client.connect();
        console.log("Conectado a MongoDB Atlas");
        const db = client.db('clinica_db');

        // --- Pacientes ---
        // fechaNacimiento (LocalDate) y fechaRegistro (Instant) van como fecha
        let pacientes = JSON.parse(fs.readFileSync('pacientes.json', 'utf8'));
        pacientes.forEach(p => {
            if (typeof p.fechaNacimiento === 'string') p.fechaNacimiento = aFecha(p.fechaNacimiento);
            if (typeof p.fechaRegistro === 'string') p.fechaRegistro = aFecha(p.fechaRegistro);
        });
        await db.collection('pacientes').drop().catch(() => {});
        await db.collection('pacientes').insertMany(pacientes);
        console.log(`${pacientes.length} pacientes importados`);

        // --- Citas ---
        // fecha y hora son texto en la entidad Cita, se dejan como vienen
        let citas = JSON.parse(fs.readFileSync('citas.json', 'utf8'));
        await db.collection('citas').drop().catch(() => {});
        await db.collection('citas').insertMany(citas);
        console.log(`${citas.length} citas importadas`);

        // --- Historiales ---
        // fecha es texto en la entidad Historial, se deja como viene
        let historiales = JSON.parse(fs.readFileSync('historiales.json', 'utf8'));
        await db.collection('historiales').drop().catch(() => {});
        await db.collection('historiales').insertMany(historiales);
        console.log(`${historiales.length} historiales importados`);

    } catch (error) {
        console.error("Error:", error);
    } finally {
        await client.close();
    }
}

importar();