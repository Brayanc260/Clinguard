const { MongoClient } = require('mongodb');
const fs = require('fs');

// Reemplaza con tu URI de conexión (la misma de application.properties)
const uri = "mongodb+srv://Admin_Clinguard:Brcamilo052606@brcamilo.k9ovmwy.mongodb.net/clinica_db?retryWrites=true&w=majority";

async function importar() {
    const client = new MongoClient(uri);
    try {
        await client.connect();
        console.log("✅ Conectado a MongoDB Atlas");
        const db = client.db('clinica_db');

        // --- Importar Pacientes ---
        let pacientes = JSON.parse(fs.readFileSync('pacientes.json', 'utf8'));
        pacientes.forEach(p => {
            // Convertir fechaRegistro de string a Date
            if (p.fechaRegistro && typeof p.fechaRegistro === 'string') {
                p.fechaRegistro = new Date(p.fechaRegistro);
            }
        });
        await db.collection('pacientes').drop().catch(() => {});
        await db.collection('pacientes').insertMany(pacientes);
        console.log(`✅ ${pacientes.length} pacientes importados`);

        // --- Importar Citas ---
        let citas = JSON.parse(fs.readFileSync('citas.json', 'utf8'));
        citas.forEach(c => {
            if (c.fecha && typeof c.fecha === 'string') {
                c.fecha = new Date(c.fecha);
            }
        });
        await db.collection('citas').drop().catch(() => {});
        await db.collection('citas').insertMany(citas);
        console.log(`✅ ${citas.length} citas importadas`);

        // --- Importar Historiales ---
        let historiales = JSON.parse(fs.readFileSync('historiales.json', 'utf8'));
        historiales.forEach(h => {
            if (h.fecha && typeof h.fecha === 'string') {
                h.fecha = new Date(h.fecha);
            }
        });
        await db.collection('historiales').drop().catch(() => {});
        await db.collection('historiales').insertMany(historiales);
        console.log(`✅ ${historiales.length} historiales importados`);

    } catch (error) {
        console.error("❌ Error:", error);
    } finally {
        await client.close();
    }
}

importar();