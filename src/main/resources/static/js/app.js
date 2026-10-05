// ==================== ESTADO GLOBAL ====================
const API = 'http://localhost:8080/api';
let token = null, uRol = null, uName = null, darkMode = true;
let curCat = 'Dosis y Peso', curTipo = 'Gramos a Miligramos';
let allPacientes = [], allMedicamentos = [], listaPacientes = [];
let pacPage = 0, pacTotalPages = 1;
let medPage = 0, medTotalPages = 1;
let histPage = 0, histTotalPages = 1;
let adminPage = 0, adminTotalPages = 1;
const PAGE_SIZE = 10;

const PAGE_TITLES = {
  dashboard: 'Dashboard',
  analisis: 'Análisis Clínico',
  conversion: 'Conversiones Médicas',
  pacientes: 'Pacientes',
  medicamentos: 'Medicamentos',
  historial: 'Mi Historial',
  reportes: 'Reportes Clínicos',
  admin: 'Panel Admin',
  perfil: 'Mi Perfil'
};

// ==================== TEMA ====================
function toggleTheme() {
  darkMode = !darkMode;
  document.documentElement.setAttribute('data-theme', darkMode ? 'dark' : 'light');
  const icon = darkMode ? '🌙' : '☀️';
  document.getElementById('theme-btn').textContent = icon;
  document.getElementById('hdr-theme').textContent = icon;
}

// ==================== SIDEBAR RESPONSIVE ====================
function toggleSb() {
  document.getElementById('sidebar').classList.toggle('open');
}

// ==================== LOGIN / REGISTRO ====================
function switchTab(t) {
  ['tab-l', 'tab-r'].forEach((id, i) => document.getElementById(id).classList.toggle('active', (t === 'login' && i === 0) || (t === 'registro' && i === 1)));
  document.getElementById('fl').style.display = t === 'login' ? 'block' : 'none';
  document.getElementById('fr').style.display = t === 'registro' ? 'block' : 'none';
  hideA();
}
function hideA() {
  ['la', 'lok'].forEach(id => {
    const e = document.getElementById(id);
    e.style.display = 'none';
    e.textContent = '';
  });
}
function showA(id, msg) {
  const e = document.getElementById(id);
  e.textContent = msg;
  e.style.display = 'block';
}

async function doLogin() {
  const nombreUsuario = document.getElementById('lu').value.trim();
  const password = document.getElementById('lp').value;
  if (!nombreUsuario || !password) {
    showA('la', 'Completa todos los campos.');
    return;
  }
  const btn = document.querySelector('#fl .btn-primary');
  btn.textContent = 'Verificando…';
  btn.disabled = true;
  try {
    const res = await fetch(`${API}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nombreUsuario, password })
    });
    if (!res.ok) {
      showA('la', 'Credenciales incorrectas. Verifica usuario y contraseña.');
      return;
    }
    const data = await res.json();
    token = data.token;
    try {
      const p = JSON.parse(atob(token.split('.')[1]));
      uRol = p.rol || p.role || p.authorities || 'USUARIO';
    } catch {
      uRol = 'USUARIO';
    }
    uName = nombreUsuario;
    localStorage.setItem('token', token);
    localStorage.setItem('uName', uName);
    initApp();
  } catch {
    showA('la', '⚠️ Error conectando. ¿Está Spring Boot en el puerto 8080?');
  } finally {
    btn.textContent = 'Autenticar →';
    btn.disabled = false;
  }
}

async function doRegistro() {
  const nombreUsuario = document.getElementById('ru').value.trim();
  const password = document.getElementById('rp').value;
  const rol = document.getElementById('rr').value;
  if (!nombreUsuario || !password) {
    showA('la', 'Completa todos los campos.');
    return;
  }
  if (password.length < 6) {
    showA('la', 'La contraseña debe tener al menos 6 caracteres.');
    return;
  }
  try {
    const res = await fetch(`${API}/auth/registro?rol=${rol}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nombreUsuario, password })
    });
    const msg = await res.text();
    res.ok ? showA('lok', msg || '✅ Usuario creado exitosamente.') : showA('la', msg || 'Error al crear usuario.');
  } catch {
    showA('la', 'Error conectando al servidor.');
  }
}

function initApp() {
  document.getElementById('login-screen').style.display = 'none';
  document.getElementById('sidebar').style.display = 'flex';
  document.getElementById('main-content').style.display = 'flex';
  document.getElementById('sb-nm').textContent = uName;
  document.getElementById('sb-rl').textContent = uRol;
  document.getElementById('sb-av').textContent = uName.substring(0, 2).toUpperCase();
  if (String(uRol).toUpperCase().includes('ADMIN')) document.getElementById('nb-admin').style.display = 'flex';
  nav('dashboard');
}

// ==================== NAVEGACIÓN ====================
function nav(p) {
  document.querySelectorAll('.page').forEach(x => x.classList.remove('active'));
  document.querySelectorAll('.nb').forEach(x => x.classList.remove('active'));
  document.getElementById('page-' + p).classList.add('active');
  document.querySelectorAll('.nb').forEach(b => {
    if (b.getAttribute('onclick') === `nav('${p}')`) b.classList.add('active');
  });
  document.getElementById('h-title').textContent = PAGE_TITLES[p] || p;
  document.getElementById('sidebar').classList.remove('open');
  if (p === 'dashboard') loadDash();
  if (p === 'analisis') loadAnalisis();
  if (p === 'historial') loadHist();
  if (p === 'reportes') loadReportes();
  if (p === 'admin') loadAdmin();
  if (p === 'pacientes') loadPacientes();
  if (p === 'medicamentos') loadMedicamentos();
  if (p === 'perfil') loadPerfil();
  if (p === 'conversion') {
    cargarPacientesParaConversion();
    cargarMedicamentosParaConversion();
    resetResultDisplay();
  }
}

// ==================== FORMATEADOR DE RESULTADOS LEGIBLES ====================
function formatResult(value, unidadDestino, tipoConversion) {
  let num = parseFloat(value);
  if (isNaN(num)) return value + ' ' + unidadDestino;

  if (tipoConversion === 'Mililitros a Litros') {
    let litros = num;
    if (litros >= 1) {
      return litros % 1 === 0 ? litros + ' L' : litros.toFixed(2) + ' L';
    } else {
      let ml = litros * 1000;
      return ml % 1 === 0 ? ml + ' mL' : ml.toFixed(2) + ' mL';
    }
  }
  if (tipoConversion === 'Litros a Mililitros') {
    let ml = num;
    return ml % 1 === 0 ? ml + ' mL' : ml.toFixed(2) + ' mL';
  }
  if (tipoConversion === 'Mililitros a Gotas') {
    return Math.round(num) + ' gotas';
  }
  if (tipoConversion === 'Gotas a Mililitros') {
    return num % 1 === 0 ? num + ' mL' : num.toFixed(2) + ' mL';
  }
  if (tipoConversion === 'CC a Mililitros') {
    return num % 1 === 0 ? num + ' mL' : num.toFixed(2) + ' mL';
  }

  let formattedNum = num % 1 === 0 ? num.toString() : num.toFixed(2);
  return formattedNum + ' ' + unidadDestino;
}

// ==================== VALIDACIÓN AUTOMÁTICA Y SUGERENCIA (SOLO DOSIS Y PESO) ====================
async function autoValidateOrSuggest() {
  const pacienteId = document.getElementById('conv-paciente-select').value;
  const medicamentoId = document.getElementById('conv-medicamento-select').value;
  const dosisInput = document.getElementById('conv-val');
  const dosisValue = dosisInput.value === '' ? NaN : parseFloat(dosisInput.value);

  document.getElementById('suggest-dose-box').style.display = 'none';

  if (curCat !== 'Dosis y Peso') {
    resetResultDisplay();
    return;
  }
  if (!pacienteId || !medicamentoId) {
    resetResultDisplay();
    return;
  }

  const paciente = allPacientes.find(p => p.id == pacienteId);
  const medicamento = allMedicamentos.find(m => m.id == medicamentoId);
  if (!paciente || !medicamento) return;
  const peso = paciente.pesoKg;
  if (!peso || peso <= 0) {
    updateResultDisplay('neutral', '⚠️ Sin peso válido', 'El paciente no tiene peso registrado.', null);
    return;
  }
  const dosisMinTotal = medicamento.dosisMinimaMgKg * peso;
  const dosisMaxTotal = medicamento.dosisMaximaMgKg * peso;
  const dosisSugerida = (dosisMinTotal + dosisMaxTotal) / 2;

  if (isNaN(dosisValue)) {
    updateResultDisplay('suggest', `${dosisSugerida.toFixed(2)} mg`, `Dosis sugerida (rango seguro: ${dosisMinTotal.toFixed(1)} - ${dosisMaxTotal.toFixed(1)} mg)`, 'suggest');
    document.getElementById('suggest-dose-value').innerText = dosisSugerida.toFixed(2);
    document.getElementById('suggest-dose-box').style.display = 'block';
    return;
  }

  if (dosisValue < dosisMinTotal) {
    updateResultDisplay('warn', `⚠️ ${dosisValue} mg`, `Infradosis: por debajo del mínimo (${dosisMinTotal.toFixed(1)} mg). Eficacia compromised.`, 'yellow');
  } else if (dosisValue > dosisMaxTotal) {
    updateResultDisplay('danger', `🚨 ${dosisValue} mg`, `Sobredosis: supera el máximo (${dosisMaxTotal.toFixed(1)} mg). Riesgo tóxico.`, 'red');
  } else {
    updateResultDisplay('safe', `✅ ${dosisValue} mg`, `Dosis SEGURA dentro del rango (${dosisMinTotal.toFixed(1)} - ${dosisMaxTotal.toFixed(1)} mg).`, 'green');
  }
}

function updateResultDisplay(type, mainValue, observation, semColor) {
  const card = document.getElementById('conv-semaforo-card');
  const resVal = document.getElementById('res-val');
  const resUnit = document.getElementById('res-unit');
  const resObs = document.getElementById('res-obs');
  card.className = `card res-card ${type}`;
  resVal.className = `res-big ${type}`;
  resVal.textContent = mainValue;
  resUnit.textContent = type === 'suggest' ? 'DOSIS SUGERIDA (promedio seguro)' : 'RESULTADO DE VALIDACIÓN';
  resObs.textContent = observation;
  resObs.className = `res-obs ${type}`;
  resObs.style.display = 'block';
  document.querySelectorAll('.sem-light').forEach(l => l.classList.remove('active-red', 'active-yellow', 'active-green', 'active-suggest'));
  if (semColor === 'red') document.getElementById('sem-red').classList.add('active-red');
  else if (semColor === 'yellow') document.getElementById('sem-yellow').classList.add('active-yellow');
  else if (semColor === 'green') document.getElementById('sem-green').classList.add('active-green');
  else if (semColor === 'suggest') document.getElementById('sem-suggest').classList.add('active-suggest');
}

function resetResultDisplay() {
  const card = document.getElementById('conv-semaforo-card');
  card.className = 'card res-card neutral';
  document.getElementById('res-val').className = 'res-big';
  document.getElementById('res-val').textContent = '—';
  document.getElementById('res-unit').textContent = 'Esperando datos…';
  document.getElementById('res-obs').style.display = 'none';
  document.querySelectorAll('.sem-light').forEach(l => l.classList.remove('active-red', 'active-yellow', 'active-green', 'active-suggest'));
  document.getElementById('suggest-dose-box').style.display = 'none';
}

function copySuggestedDose() {
  const suggestedVal = document.getElementById('suggest-dose-value').innerText;
  if (suggestedVal && suggestedVal !== '—') {
    document.getElementById('conv-val').value = suggestedVal;
    autoValidateOrSuggest();
  }
}

// ==================== CONVERSIÓN MANUAL (RESPETA CATEGORÍA) ====================
async function manualConversion() {
  const pacienteId = document.getElementById('conv-paciente-select').value;
  const medicamentoId = document.getElementById('conv-medicamento-select').value;
  let valor = parseFloat(document.getElementById('conv-val').value);
  const errorBanner = document.getElementById('conv-error-banner');
  errorBanner.style.display = 'none';

  if (curCat === 'Dosis y Peso' && pacienteId && medicamentoId) {
    if (isNaN(valor)) {
      const paciente = allPacientes.find(p => p.id == pacienteId);
      const medicamento = allMedicamentos.find(m => m.id == medicamentoId);
      if (paciente && medicamento && paciente.pesoKg) {
        valor = ((medicamento.dosisMinimaMgKg + medicamento.dosisMaximaMgKg) / 2) * paciente.pesoKg;
        document.getElementById('conv-val').value = valor;
      } else {
        document.getElementById('conv-error-msg').textContent = 'Complete los datos o ingrese un valor.';
        errorBanner.style.display = 'block';
        return;
      }
    }
    if (isNaN(valor) || valor <= 0) {
      document.getElementById('conv-error-msg').textContent = 'Ingrese un valor numérico mayor a 0.';
      errorBanner.style.display = 'block';
      return;
    }
    try {
      const res = await fetch(`${API}/conversiones/validar-dosis`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
        body: JSON.stringify({ pacienteId: parseInt(pacienteId), medicamentoId: parseInt(medicamentoId), dosisRecetada: valor })
      });
      if (!res.ok) throw new Error(await res.text());
      autoValidateOrSuggest();
      loadDash();
      loadHist();
    } catch (err) {
      document.getElementById('conv-error-msg').textContent = 'Error en la validación.';
      document.getElementById('conv-error-code').textContent = err.message;
      errorBanner.style.display = 'block';
    }
    return;
  }

  if (isNaN(valor) || valor <= 0) {
    document.getElementById('conv-error-msg').textContent = 'Ingrese un valor numérico mayor a 0.';
    errorBanner.style.display = 'block';
    return;
  }
  try {
    const res = await fetch(`${API}/conversiones`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify({ categoria: curCat, tipoConversion: curTipo, valor })
    });
    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();
    let salida = data.valorSalida ?? data.valorResultante ?? data.resultado;
    let unidad = data.unidadDestino ?? data.unidad ?? '';
    let formatted = formatResult(salida, unidad, curTipo);
    document.getElementById('res-val').textContent = formatted;
    document.getElementById('res-unit').textContent = 'RESULTADO';
    document.getElementById('conv-semaforo-card').className = 'card res-card neutral';
    document.getElementById('res-obs').style.display = 'none';
    document.querySelectorAll('.sem-light').forEach(l => l.classList.remove('active-red', 'active-yellow', 'active-green', 'active-suggest'));
    
    const stamp = document.getElementById('res-stamp');
    stamp.innerHTML = '📝 Cálculo registrado bajo responsabilidad de: <span class="stamp-user">' + uName + '</span><br><span class="stamp-date">' + new Date().toLocaleString() + '</span>';
    stamp.style.display = 'block';
    loadDash();
    loadHist();
  } catch (err) {
    document.getElementById('conv-error-msg').textContent = 'Error en la conversión.';
    document.getElementById('conv-error-code').textContent = err.message;
    errorBanner.style.display = 'block';
  }
}

// ==================== CRUD PACIENTES ====================
async function loadPacientes() {
  try {
    const res = await fetch(`${API}/pacientes/paged?page=${pacPage}&size=${PAGE_SIZE}`, { 
      headers: { 'Authorization': 'Bearer ' + token } 
    });
    if (!res.ok) return;
    const data = await res.json();
    allPacientes = data.content;
    pacTotalPages = data.totalPages || 1;
    renderPacientes(allPacientes);
    document.getElementById('m-pac').textContent = data.totalElements || 0;
    updatePaginationUI('pac', pacPage, pacTotalPages);
    document.getElementById('pac-pagination').style.display = pacTotalPages > 1 ? 'flex' : 'none';
  } catch (e) { }
}

function renderPacientes(data) {
  const tb = document.getElementById('pac-tb');
  if (!data.length) {
    tb.innerHTML = '<tr class="empty-row"><td colspan="6">No hay pacientes registrados.</td></tr>';
    return;
  }
  tb.innerHTML = data.map(p => `
    <tr>
      <td style="font-family:'DM Mono'; color:var(--text2);">#${p.id}</td>
      <td><b>${p.nombre}</b></td>
      <td style="font-family:'DM Mono';">${p.identificacion}</td>
      <td>${p.pesoKg ?? '—'} kg</td>
      <td>${p.edad ?? '—'} años</td>
      <td class="actions">
        <button type="button" class="act-btn" onclick="editPaciente(${p.id})" title="Editar">✏️</button>
        <button type="button" class="act-btn" onclick="confirmDelete('paciente',${p.id},'${p.nombre}')" title="Eliminar">🗑</button>
      </td>
    </tr>
  `).join('');
}

function filterPacientes() {
  const q = document.getElementById('pac-search').value.toLowerCase();
  renderPacientes(allPacientes.filter(p => p.nombre.toLowerCase().includes(q) || String(p.identificacion).toLowerCase().includes(q)));
}

function editPaciente(id) {
  const p = allPacientes.find(x => x.id === id);
  if (!p) return;
  document.getElementById('pac-id').value = p.id;
  document.getElementById('pac-nombre').value = p.nombre;
  document.getElementById('pac-ident').value = p.identificacion;
  document.getElementById('pac-peso').value = p.pesoKg;
  document.getElementById('pac-edad').value = p.edad;
  document.getElementById('pac-modal-title').textContent = 'Editar paciente';
  document.getElementById('pac-alert').style.display = 'none';
  document.getElementById('modal-paciente').classList.add('show');
}

async function savePaciente() {
  const id = document.getElementById('pac-id').value;
  const body = {
    nombre: document.getElementById('pac-nombre').value.trim(),
    identificacion: document.getElementById('pac-ident').value.trim(),
    pesoKg: parseFloat(document.getElementById('pac-peso').value),
    edad: parseInt(document.getElementById('pac-edad').value)
  };
  if (!body.nombre || !body.identificacion || isNaN(body.pesoKg) || body.pesoKg <= 0) {
    const e = document.getElementById('pac-alert');
    e.textContent = 'Completa los campos requeridos (nombre, identificación y peso > 0).';
    e.style.display = 'block';
    return;
  }
  try {
    const res = await fetch(id ? `${API}/pacientes/${id}` : `${API}/pacientes`, {
      method: id ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(body)
    });
    if (!res.ok) {
      const t = await res.text();
      const e = document.getElementById('pac-alert');
      e.textContent = t || 'Error.';
      e.style.display = 'block';
      return;
    }
    closeModal('paciente');
    await loadPacientes();
    await cargarPacientesParaConversion();
  } catch {
    Swal.fire('Error', 'No se pudo conectar con el servidor.', 'error');
  }
}

// ==================== CRUD MEDICAMENTOS ====================
async function loadMedicamentos() {
  try {
    const res = await fetch(`${API}/medicamentos/paged?page=${medPage}&size=${PAGE_SIZE}`, { 
      headers: { 'Authorization': 'Bearer ' + token } 
    });
    if (!res.ok) return;
    const data = await res.json();
    allMedicamentos = data.content;
    medTotalPages = data.totalPages || 1;
    renderMedicamentos(allMedicamentos);
    document.getElementById('m-med').textContent = data.totalElements || 0;
    updatePaginationUI('med', medPage, medTotalPages);
    document.getElementById('med-pagination').style.display = medTotalPages > 1 ? 'flex' : 'none';
  } catch (e) { }
}

function renderMedicamentos(data) {
  const tb = document.getElementById('med-tb');
  if (!data.length) {
    tb.innerHTML = '<tr class="empty-row"><td colspan="6">No hay medicamentos registrados.</td></tr>';
    return;
  }
  tb.innerHTML = data.map(m => `
    <tr>
      <td style="font-family:'DM Mono'; color:var(--text2);">#${m.id}</td>
      <td><b>${m.nombre}</b></td>
      <td>${m.dosisMinimaMgKg ?? '—'} mg/kg</td>
      <td>${m.dosisMaximaMgKg ?? '—'} mg/kg</td>
      <td style="font-size:12px; color:var(--text2); max-width:200px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;" title="${m.descripcion ?? ''}">${m.descripcion ?? '—'}</td>
      <td class="actions">
        <button type="button" class="act-btn" onclick="editMedicamento(${m.id})" title="Editar">✏️</button>
        <button type="button" class="act-btn" onclick="confirmDelete('medicamento',${m.id},'${m.nombre}')" title="Eliminar">🗑</button>
      </td>
    </tr>
  `).join('');
}

function filterMedicamentos() {
  const q = document.getElementById('med-search').value.toLowerCase();
  renderMedicamentos(allMedicamentos.filter(m => m.nombre.toLowerCase().includes(q)));
}

function editMedicamento(id) {
  const m = allMedicamentos.find(x => x.id === id);
  if (!m) return;
  document.getElementById('med-id').value = m.id;
  document.getElementById('med-nombre').value = m.nombre;
  document.getElementById('med-min').value = m.dosisMinimaMgKg;
  document.getElementById('med-max').value = m.dosisMaximaMgKg;
  document.getElementById('med-desc').value = m.descripcion || '';
  document.getElementById('med-modal-title').textContent = 'Editar medicamento';
  document.getElementById('med-alert').style.display = 'none';
  document.getElementById('modal-medicamento').classList.add('show');
}

async function saveMedicamento() {
  const id = document.getElementById('med-id').value;
  const body = {
    nombre: document.getElementById('med-nombre').value.trim(),
    dosisMinimaMgKg: parseFloat(document.getElementById('med-min').value),
    dosisMaximaMgKg: parseFloat(document.getElementById('med-max').value),
    descripcion: document.getElementById('med-desc').value
  };
  if (!body.nombre || isNaN(body.dosisMinimaMgKg) || isNaN(body.dosisMaximaMgKg) || body.dosisMaximaMgKg <= body.dosisMinimaMgKg) {
    const e = document.getElementById('med-alert');
    e.textContent = 'Completa todos los campos. La dosis máxima debe ser mayor que la mínima.';
    e.style.display = 'block';
    return;
  }
  try {
    const res = await fetch(id ? `${API}/medicamentos/${id}` : `${API}/medicamentos`, {
      method: id ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      body: JSON.stringify(body)
    });
    if (!res.ok) {
      const t = await res.text();
      const e = document.getElementById('med-alert');
      e.textContent = t || 'Error.';
      e.style.display = 'block';
      return;
    }
    closeModal('medicamento');
    await loadMedicamentos();
    await cargarMedicamentosParaConversion();
  } catch {
    Swal.fire('Error', 'No se pudo conectar con el servidor.', 'error');
  }
}

// ==================== CONFIRMAR ELIMINAR ====================
function confirmDelete(type, id, name) {
  document.getElementById('confirm-text').textContent = `¿Eliminar "${name}"? Esta acción no se puede deshacer.`;
  document.getElementById('confirm-btn').onclick = () => doDelete(type, id);
  document.getElementById('modal-confirm').classList.add('show');
}

async function doDelete(type, id) {
  try {
    await fetch(`${API}/${type === 'paciente' ? 'pacientes' : 'medicamentos'}/${id}`, {
      method: 'DELETE',
      headers: { 'Authorization': 'Bearer ' + token }
    });
    closeModal('confirm');
    if (type === 'paciente') loadPacientes();
    else loadMedicamentos();
  } catch {
    Swal.fire('Error', 'Error al eliminar el registro.', 'error');
  }
}

// ==================== HISTORIAL, ADMIN, DASHBOARD, PERFIL ====================
async function loadHist() {
  try {
    const res = await fetch(`${API}/conversiones/historial/paged?page=${histPage}&size=${PAGE_SIZE}`, { 
      headers: { 'Authorization': 'Bearer ' + token } 
    });
    if (!res.ok) return;
    const data = await res.json();
    const items = data.content;
    histTotalPages = data.totalPages || 1;
    document.getElementById('hist-tb').innerHTML = items.length
      ? items.map(r => `<tr><td><b>${r.tipoConversion}</b></td><td style="font-family:'DM Mono';">${r.valorEntrada}</td><td style="font-family:'DM Mono'; color:var(--accent);">${r.valorSalida}</td><td style="color:var(--text2);">${fmt(r.fechaHora)}</td></tr>`).join('')
      : '<tr class="empty-row"><td colspan="4">Sin registros.</td></tr>';
    updatePaginationUI('hist', histPage, histTotalPages);
    document.getElementById('hist-pagination').style.display = histTotalPages > 1 ? 'flex' : 'none';
  } catch (e) { }
}

async function loadAdmin() {
  try {
    const res = await fetch(`${API}/conversiones/historial/all/paged?page=${adminPage}&size=${PAGE_SIZE}`, { 
      headers: { 'Authorization': 'Bearer ' + token } 
    });
    if (!res.ok) {
      document.getElementById('admin-tb').innerHTML = '<tr class="empty-row"><td colspan="5">Acceso denegado.</td></tr>';
      return;
    }
    const data = await res.json();
    const items = data.content;
    adminTotalPages = data.totalPages || 1;
    document.getElementById('admin-tb').innerHTML = items.length
      ? items.map(r => `<tr><td><code style="color:var(--accent);">${r.usuarioNombre}</code></td><td><b>${r.tipoConversion}</b></td><td style="font-family:'DM Mono';">${r.valorEntrada}</td><td style="font-family:'DM Mono'; color:var(--accent);">${r.valorSalida}</td><td style="color:var(--text2);">${fmt(r.fechaHora)}</td></tr>`).join('')
      : '<tr class="empty-row"><td colspan="5">Sin registros.</td></tr>';
    updatePaginationUI('admin', adminPage, adminTotalPages);
    document.getElementById('admin-pagination').style.display = adminTotalPages > 1 ? 'flex' : 'none';
  } catch (e) { }
}

async function loadDash() {
  try {
    const [rH, rP, rM] = await Promise.all([
      fetch(`${API}/conversiones/historial`, { headers: { 'Authorization': 'Bearer ' + token } }),
      fetch(`${API}/pacientes`, { headers: { 'Authorization': 'Bearer ' + token } }),
      fetch(`${API}/medicamentos`, { headers: { 'Authorization': 'Bearer ' + token } })
    ]);
    const hist = rH.ok ? await rH.json() : [];
    const pacs = rP.ok ? await rP.json() : [];
    const meds = rM.ok ? await rM.json() : [];
    const hoy = new Date().toDateString();
    document.getElementById('m-hoy').textContent = hist.filter(r => new Date(r.fechaHora).toDateString() === hoy).length;
    document.getElementById('m-tot').textContent = hist.length;
    document.getElementById('m-pac').textContent = pacs.length;
    document.getElementById('m-med').textContent = meds.length;
    buildChart(hist);
    if (hist.length) {
      const freq = {};
      hist.forEach(r => freq[r.tipoConversion] = (freq[r.tipoConversion] || 0) + 1);
      const sorted = Object.entries(freq).sort((a, b) => b[1] - a[1]).slice(0, 5);
      const max = sorted[0][1];
      document.getElementById('top-tipos').innerHTML = sorted.map(([t, c]) => `
        <div class="p-row">
          <div style="display:flex; justify-content:space-between; font-size:13px; margin-bottom:4px;">
            <span>${t}</span><span style="font-family:'DM Mono'; font-weight:600;">${c}×</span>
          </div>
          <div class="p-bar"><div class="p-fill" style="width:${(c / max) * 100}%;"></div></div>
        </div>`).join('');
    } else {
      document.getElementById('top-tipos').innerHTML = '<p style="color:var(--text2); font-size:13px;">Sin datos aún.</p>';
    }
    document.getElementById('dash-tb').innerHTML = hist.slice(0, 5).map(r =>
      `<tr><td><b>${r.tipoConversion}</b></td><td style="font-family:'DM Mono';">${r.valorEntrada}</td><td style="font-family:'DM Mono'; color:var(--accent);">${r.valorSalida}</td><td style="color:var(--text2);">${fmt(r.fechaHora)}</td></tr>`
    ).join('') || '<tr class="empty-row"><td colspan="4">Sin actividad reciente.</td></tr>';
  } catch (e) { }
}

function buildChart(hist) {
  const days = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
  const counts = Array(7).fill(0);
  const now = new Date();
  hist.forEach(r => {
    const d = new Date(r.fechaHora);
    const diff = Math.floor((now - d) / (1000 * 60 * 60 * 24));
    if (diff >= 0 && diff < 7) counts[6 - diff]++;
  });
  const max = Math.max(...counts, 1);
  const labels = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(now);
    d.setDate(d.getDate() - (6 - i));
    return days[d.getDay()];
  });
  document.getElementById('chart').innerHTML = counts.map((c, i) =>
    `<div class="chart-bar-wrap">
      <div class="chart-bar" style="height: ${Math.max((c / max) * 110, 6)}px;">
        <div class="chart-val">${c}</div>
      </div>
      <div class="chart-lbl">${labels[i]}</div>
    </div>`
  ).join('');
}

async function loadPerfil() {
  document.getElementById('prof-av').textContent = uName.substring(0, 2).toUpperCase();
  document.getElementById('prof-nm').textContent = uName;
  document.getElementById('prof-rl').textContent = uRol;
  document.getElementById('prof-user').textContent = uName;
  document.getElementById('prof-role').textContent = uRol;
  try {
    const res = await fetch(`${API}/conversiones/historial`, { headers: { 'Authorization': 'Bearer ' + token } });
    if (!res.ok) return;
    const data = await res.json();
    const hoy = new Date().toDateString();
    const sem = new Date();
    sem.setDate(sem.getDate() - 7);
    document.getElementById('prof-tot').textContent = data.length;
    document.getElementById('prof-hoy').textContent = data.filter(r => new Date(r.fechaHora).toDateString() === hoy).length;
    document.getElementById('prof-sem').textContent = data.filter(r => new Date(r.fechaHora) >= sem).length;
    document.getElementById('prof-last').textContent = data.length ? fmt(data[0].fechaHora) : 'Sin conversiones';
    document.getElementById('prof-tb').innerHTML = data.slice(0, 5).map(r =>
      `<tr><td><b>${r.tipoConversion}</b></td><td style="font-family:'DM Mono';">${r.valorEntrada}</td><td style="font-family:'DM Mono'; color:var(--accent);">${r.valorSalida}</td><td style="color:var(--text2);">${fmt(r.fechaHora)}</td></tr>`
    ).join('') || '<tr class="empty-row"><td colspan="4">Sin conversiones.</td></tr>';
  } catch (e) { }
}

// ==================== CARGAR COMBOS PARA CONVERSIÓN ====================
async function cargarPacientesParaConversion() {
  try {
    if (!token) return;
    const res = await fetch(`${API}/pacientes`, { headers: { 'Authorization': 'Bearer ' + token } });
    if (!res.ok) return;
    listaPacientes = await res.json();
    actualizarSelectPacientes(listaPacientes);
    allPacientes = listaPacientes;
  } catch (e) {
    console.error('Error cargando pacientes para conversión', e);
  }
}

function filtrarPaciente() {
  const txt = document.getElementById('conv-buscar-paciente').value.toLowerCase();
  actualizarSelectPacientes(listaPacientes.filter(p =>
    p.nombre.toLowerCase().includes(txt) || String(p.identificacion).toLowerCase().includes(txt)
  ));
}

function actualizarSelectPacientes(lista) {
  const sel = document.getElementById('conv-paciente-select');
  if (!sel) return;
  const prev = sel.value;
  sel.innerHTML = '<option value="">— Sin paciente —</option>';
  lista.forEach(p => {
    const opt = document.createElement('option');
    opt.value = p.id;
    opt.textContent = `${p.nombre} (${p.pesoKg} kg) — ${p.identificacion}`;
    sel.appendChild(opt);
  });
  if (prev) sel.value = prev;
}

async function cargarMedicamentosParaConversion() {
  try {
    if (!token) return;
    const res = await fetch(`${API}/medicamentos`, { headers: { 'Authorization': 'Bearer ' + token } });
    if (!res.ok) return;
    const meds = await res.json();
    allMedicamentos = meds;
    const sel = document.getElementById('conv-medicamento-select');
    if (!sel) return;
    const prev = sel.value;
    sel.innerHTML = '<option value="">— Sin medicamento —</option>';
    meds.forEach(m => {
      const opt = document.createElement('option');
      opt.value = m.id;
      opt.textContent = `${m.nombre} (${m.dosisMinimaMgKg}–${m.dosisMaximaMgKg} mg/kg)`;
      sel.appendChild(opt);
    });
    if (prev) sel.value = prev;
  } catch (e) {
    console.error('Error cargando medicamentos para conversión', e);
  }
}

// ==================== CATEGORÍAS Y OPCIONES ====================
function selectCat(el, cat) {
  document.querySelectorAll('.cat').forEach(c => c.classList.remove('active'));
  el.classList.add('active');
  curCat = cat;
  const ids = { 'Dosis y Peso': 'DosisPeso', 'Volumen': 'Volumen', 'Laboratorio': 'Laboratorio' };
  ['DosisPeso', 'Volumen', 'Laboratorio'].forEach(k => {
    const g = document.getElementById('opts-' + k);
    if (g) g.style.display = (ids[cat] === k) ? 'grid' : 'none';
  });
  const first = document.querySelector('#opts-' + ids[cat] + ' .copt');
  if (first) {
    document.querySelectorAll('#opts-' + ids[cat] + ' .copt').forEach(o => o.classList.remove('sel'));
    first.classList.add('sel');
    const m = first.getAttribute('onclick').match(/'([^']+)'\)/);
    if (m) {
      curTipo = m[1];
      updateLabel();
    }
  }
  resetResultDisplay();
  autoValidateOrSuggest();
}

function selOpt(el, tipo) {
  el.closest('.conv-opts').querySelectorAll('.copt').forEach(o => o.classList.remove('sel'));
  el.classList.add('sel');
  curTipo = tipo;
  updateLabel();
  resetResultDisplay();
  autoValidateOrSuggest();
}

function updateLabel() {
  const lbl = document.getElementById('conv-lbl');
  const hasPac = document.getElementById('conv-paciente-select').value;
  const hasMed = document.getElementById('conv-medicamento-select').value;
  if (curCat === 'Dosis y Peso' && hasPac && hasMed) {
    lbl.textContent = 'C) Dosis recetada (mg) a validar';
  } else {
    const labelMap = {
      'Gramos a Miligramos': 'Valor en gramos (g)',
      'Miligramos a Gramos': 'Valor en miligramos (mg)',
      'Miligramos a Microgramos': 'Valor en miligramos (mg)',
      'Kilogramos a Gramos': 'Valor en kilogramos (kg)',
      'Calcular Dosis por Peso (mg/kg)': 'Peso del paciente (kg)',
      'Mililitros a Litros': 'Valor en mililitros (mL)',
      'Litros a Mililitros': 'Valor en litros (L)',
      'Mililitros a Gotas': 'Valor en mililitros (mL)',
      'Gotas a Mililitros': 'Valor en gotas (gts)',
      'CC a Mililitros': 'Valor en CC',
      'Glucosa mmol/L a mg/dL': 'Glucosa en mmol/L',
      'Glucosa mg/dL a mmol/L': 'Glucosa en mg/dL',
      'UI a Miligramos': 'Valor en UI',
      'Colesterol mmol/L a mg/dL': 'Colesterol en mmol/L',
      'Creatinina mg/dL a µmol/L': 'Creatinina en mg/dL'
    };
    lbl.textContent = 'C) ' + (labelMap[curTipo] || 'Valor a convertir');
  }
}

function openModal(type) {
  if (type === 'paciente') {
    ['pac-id', 'pac-nombre', 'pac-ident', 'pac-peso', 'pac-edad'].forEach(id => document.getElementById(id).value = '');
    document.getElementById('pac-modal-title').textContent = 'Nuevo paciente';
    document.getElementById('pac-alert').style.display = 'none';
  } else {
    ['med-id', 'med-nombre', 'med-min', 'med-max', 'med-desc'].forEach(id => document.getElementById(id).value = '');
    document.getElementById('med-modal-title').textContent = 'Nuevo medicamento';
    document.getElementById('med-alert').style.display = 'none';
  }
  document.getElementById('modal-' + type).classList.add('show');
}

function closeModal(type) {
  document.getElementById('modal-' + type).classList.remove('show');
}

function doLogout() {
  token = null;
  uRol = null;
  uName = null;
  localStorage.removeItem('token');
  localStorage.removeItem('uName');
  document.getElementById('sidebar').style.display = 'none';
  document.getElementById('main-content').style.display = 'none';
  document.getElementById('login-screen').style.display = 'flex';
  document.getElementById('lu').value = '';
  document.getElementById('lp').value = '';
  resetResultDisplay();
}

function fmt(d) {
  if (!d) return '—';
  return new Date(d).toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
}

// Cerrar modales al clic fuera
document.querySelectorAll('.modal-bg').forEach(m => {
  m.addEventListener('click', e => {
    if (e.target === m) m.classList.remove('show');
  });
});

// Cerrar sidebar en resize
window.addEventListener('resize', () => {
  if (window.innerWidth > 768) document.getElementById('sidebar').classList.remove('open');
});

// Restaurar sesión al recargar
document.addEventListener('DOMContentLoaded', () => {
  const savedToken = localStorage.getItem('token');
  const savedUser = localStorage.getItem('uName');
  if (savedToken && savedUser) {
    token = savedToken;
    uName = savedUser;
    try {
      const p = JSON.parse(atob(token.split('.')[1]));
      uRol = p.rol || p.role || p.authorities || 'USUARIO';
      initApp();
    } catch (e) {
      doLogout();
    }
  }
});

// ==================== PAGINATION HANDLERS ====================
function updatePaginationUI(prefix, page, total) {
  const info = document.getElementById(prefix + '-page-info');
  const btnPrev = document.getElementById(prefix + '-prev');
  const btnNext = document.getElementById(prefix + '-next');
  if(info) info.textContent = (page + 1) + ' / ' + total;
  if(btnPrev) btnPrev.disabled = (page <= 0);
  if(btnNext) btnNext.disabled = (page >= total - 1);
}

function changePacPage(delta) {
  if ((delta < 0 && pacPage > 0) || (delta > 0 && pacPage < pacTotalPages - 1)) {
    pacPage += delta;
    loadPacientes();
  }
}

function changeMedPage(delta) {
  if ((delta < 0 && medPage > 0) || (delta > 0 && medPage < medTotalPages - 1)) {
    medPage += delta;
    loadMedicamentos();
  }
}

function changeHistPage(delta) {
  if ((delta < 0 && histPage > 0) || (delta > 0 && histPage < histTotalPages - 1)) {
    histPage += delta;
    loadHist();
  }
}

function changeAdminPage(delta) {
  if ((delta < 0 && adminPage > 0) || (delta > 0 && adminPage < adminTotalPages - 1)) {
    adminPage += delta;
    loadAdmin();
  }
}

// ==================== ANÁLISIS CLÍNICO ====================
async function loadAnalisis() {
  try {
    // Cargar datos en paralelo
    const [resPac, resMed, resHist] = await Promise.all([
      fetch(`${API}/pacientes`, { headers: { 'Authorization': 'Bearer ' + token } }),
      fetch(`${API}/medicamentos`, { headers: { 'Authorization': 'Bearer ' + token } }),
      fetch(`${API}/conversiones/historial`, { headers: { 'Authorization': 'Bearer ' + token } })
    ]);

    const pacientes = resPac.ok ? await resPac.json() : [];
    const medicamentos = resMed.ok ? await resMed.json() : [];
    const historial = resHist.ok ? await resHist.json() : [];

    // ---- KPIs ----
    document.getElementById('kpi-pac').textContent = pacientes.length;
    document.getElementById('kpi-pac-trend').textContent = pacientes.length > 0 ? `${pacientes.length} en el sistema` : 'Sin registros';

    document.getElementById('kpi-conv').textContent = historial.length;
    const hoy = new Date().toDateString();
    const convHoy = historial.filter(r => new Date(r.fechaHora).toDateString() === hoy).length;
    const convTrend = document.getElementById('kpi-conv-trend');
    convTrend.textContent = convHoy > 0 ? `+${convHoy} hoy` : 'Sin conversiones hoy';
    convTrend.className = 'analysis-trend ' + (convHoy > 0 ? 'positive' : 'neutral');

    document.getElementById('kpi-med').textContent = medicamentos.length;

    // Alertas = sobredosis + subdosis
    let alertasCount = 0;
    historial.forEach(r => {
      const obs = (r.observacion || '').toLowerCase();
      if (obs.includes('sobredosis') || obs.includes('exceso') || obs.includes('alta') ||
          obs.includes('subdosis') || obs.includes('baja') || obs.includes('insuficiente')) alertasCount++;
    });
    document.getElementById('kpi-alertas').textContent = alertasCount;
    const alertTrend = document.getElementById('kpi-alertas-trend');
    alertTrend.textContent = alertasCount > 0 ? `${alertasCount} requieren revisión` : 'Sin alertas activas';
    alertTrend.className = 'analysis-trend ' + (alertasCount > 0 ? 'warning' : 'positive');

    // ---- Gráfico de barras: últimas 6 semanas ----
    const semanas = [];
    for (let i = 5; i >= 0; i--) {
      const desde = new Date();
      const hasta = new Date();
      desde.setDate(desde.getDate() - (i + 1) * 7);
      hasta.setDate(hasta.getDate() - i * 7);
      const label = `S${6 - i}`;
      const count = historial.filter(r => {
        const d = new Date(r.fechaHora);
        return d >= desde && d < hasta;
      }).length;
      semanas.push({ label, count });
    }
    const maxSem = Math.max(...semanas.map(s => s.count), 1);
    const yTop = Math.ceil(maxSem * 1.2) || 1;

    const yLabels = document.getElementById('analisis-y-labels');
    if (yLabels) {
      yLabels.innerHTML = `<span>${yTop}</span><span>${Math.round(yTop * 0.67)}</span><span>${Math.round(yTop * 0.33)}</span><span>0</span>`;
    }

    const barPlot = document.getElementById('analisis-bar-plot');
    if (barPlot) {
      barPlot.innerHTML = `<div class="analysis-grid-lines" aria-hidden="true"><i></i><i></i><i></i><i></i></div>` +
        semanas.map(s => {
          const pct = Math.max(Math.round((s.count / yTop) * 100), s.count > 0 ? 5 : 0);
          return `<div class="analysis-bar-item">
            <span class="analysis-bar-value">${s.count}</span>
            <div class="analysis-bar-fill" style="height:${pct}%"></div>
            <small>${s.label}</small>
          </div>`;
        }).join('');
    }

    // ---- Donut chart: distribución semáforo ----
    let verde = 0, rojo = 0, amarillo = 0, morado = 0;
    historial.forEach(r => {
      const obs = (r.observacion || '').toLowerCase();
      if (obs.includes('sobredosis') || obs.includes('exceso') || obs.includes('alta')) rojo++;
      else if (obs.includes('subdosis') || obs.includes('baja') || obs.includes('insuficiente')) amarillo++;
      else if (obs.includes('sugerida') || obs.includes('sugerido')) morado++;
      else verde++;
    });
    const totalValid = verde + rojo + amarillo + morado;

    const pctVerde    = totalValid > 0 ? Math.round((verde / totalValid) * 100) : 0;
    const pctRojo     = totalValid > 0 ? Math.round((rojo / totalValid) * 100) : 0;
    const pctAmarillo = totalValid > 0 ? Math.round((amarillo / totalValid) * 100) : 0;
    const pctMorado   = totalValid > 0 ? Math.round((morado / totalValid) * 100) : 0;

    const donut = document.getElementById('analisis-donut');
    if (donut) {
      const vS = pctVerde, rS = vS + pctRojo, aS = rS + pctAmarillo;
      donut.style.background = totalValid > 0
        ? `conic-gradient(var(--safe) 0 ${vS}%, var(--danger) ${vS}% ${rS}%, var(--warn) ${rS}% ${aS}%, var(--suggest) ${aS}% 100%)`
        : `conic-gradient(var(--border) 0 100%)`;
    }
    const donutTotal = document.getElementById('analisis-donut-total');
    if (donutTotal) donutTotal.textContent = totalValid;

    document.getElementById('leg-verde').textContent    = pctVerde + '%';
    document.getElementById('leg-rojo').textContent     = pctRojo + '%';
    document.getElementById('leg-amarillo').textContent = pctAmarillo + '%';
    document.getElementById('leg-morado').textContent   = pctMorado + '%';

    // ---- Ranking de tipos de conversión ----
    const tipoCount = {};
    historial.forEach(r => {
      const t = r.tipoConversion || 'Otro';
      tipoCount[t] = (tipoCount[t] || 0) + 1;
    });
    const tiposOrdenados = Object.entries(tipoCount).sort((a, b) => b[1] - a[1]).slice(0, 5);
    const maxTipo = tiposOrdenados.length > 0 ? tiposOrdenados[0][1] : 1;
    const rankEl = document.getElementById('analisis-ranking');
    if (rankEl) {
      if (tiposOrdenados.length === 0) {
        rankEl.innerHTML = '<div style="color:var(--text2); font-size:12px;">Sin conversiones aún.</div>';
      } else {
        rankEl.innerHTML = tiposOrdenados.map(([tipo, cnt]) => {
          const pct = Math.round((cnt / historial.length) * 100);
          const barW = Math.round((cnt / maxTipo) * 100);
          return `<div class="analysis-rank-row">
            <div class="analysis-rank-label"><span>${tipo}</span><strong>${pct}%</strong></div>
            <div class="analysis-progress"><span style="width:${barW}%"></span></div>
          </div>`;
        }).join('');
      }
    }

    // ---- Tabla resumen ----
    const semAnt = new Date(); semAnt.setDate(semAnt.getDate() - 7);
    const convSemana = historial.filter(r => new Date(r.fechaHora) >= semAnt).length;
    const tb = document.getElementById('analisis-resumen-tb');
    if (tb) {
      tb.innerHTML = `
        <tr>
          <td>Pacientes registrados</td>
          <td style="font-family:'DM Mono'; font-weight:700;">${pacientes.length}</td>
          <td><span class="status status-success">Activo</span></td>
        </tr>
        <tr>
          <td>Conversiones totales</td>
          <td style="font-family:'DM Mono'; font-weight:700;">${historial.length}</td>
          <td><span class="badge badge-blue">Sistema</span></td>
        </tr>
        <tr>
          <td>Conversiones esta semana</td>
          <td style="font-family:'DM Mono'; font-weight:700;">${convSemana}</td>
          <td><span class="status ${convSemana > 0 ? 'status-success' : 'status-warning'}">${convSemana > 0 ? 'En uso' : 'Sin actividad'}</span></td>
        </tr>
        <tr>
          <td>Alertas clínicas activas</td>
          <td style="font-family:'DM Mono'; font-weight:700;">${alertasCount}</td>
          <td><span class="status ${alertasCount === 0 ? 'status-success' : 'status-danger'}">${alertasCount === 0 ? 'Sin alertas' : 'Revisar'}</span></td>
        </tr>
        <tr>
          <td>Medicamentos en catálogo</td>
          <td style="font-family:'DM Mono'; font-weight:700;">${medicamentos.length}</td>
          <td><span class="badge badge-purple">Vigente</span></td>
        </tr>
      `;
    }

  } catch (e) {
    console.error('Error en loadAnalisis:', e);
  }
}

// ==================== REPORTES CLÍNICOS ====================
async function loadReportes() {
  try {
    const periodoElem = document.getElementById('rep-periodo');
    const periodo = periodoElem ? periodoElem.value : 'todos';
    const res = await fetch(`${API}/conversiones/historial`, { headers: { 'Authorization': 'Bearer ' + token } });
    if (!res.ok) return;
    let data = await res.json();
    
    if (periodo !== 'todos') {
      const dias = parseInt(periodo);
      const limite = new Date();
      limite.setDate(limite.getDate() - dias);
      data = data.filter(r => new Date(r.fechaHora) >= limite);
    }

    let verde = 0, rojo = 0, amarillo = 0, morado = 0;
    data.forEach(r => {
      const obs = (r.observacion || '').toLowerCase();
      if (obs.includes('segura') || obs.includes('adecuada') || obs.includes('rango seguro')) verde++;
      else if (obs.includes('sobredosis') || obs.includes('exceso') || obs.includes('alta')) rojo++;
      else if (obs.includes('subdosis') || obs.includes('baja') || obs.includes('insuficiente')) amarillo++;
      else if (obs.includes('sugerida') || obs.includes('sugerido')) morado++;
      else verde++;
    });

    document.getElementById('rep-kpi-verde').textContent = verde;
    document.getElementById('rep-kpi-rojo').textContent = rojo;
    document.getElementById('rep-kpi-amarillo').textContent = amarillo;
    document.getElementById('rep-kpi-morado').textContent = morado;

    const tb = document.getElementById('rep-tb');
    if (!tb) return;
    if (!data.length) {
      tb.innerHTML = '<tr class="empty-row"><td colspan="5">No hay registros de conversiones para este período.</td></tr>';
      return;
    }

    tb.innerHTML = data.map(r => `
      <tr>
        <td><span class="stamp-user">📝 ${r.usuarioNombre || 'Usuario_Local'}</span></td>
        <td><b>${r.tipoConversion || 'Conversión'}</b></td>
        <td style="font-family:'DM Mono';">${r.valorEntrada || '-'}</td>
        <td style="font-family:'DM Mono'; color:var(--accent);">${r.valorSalida || '-'}</td>
        <td style="color:var(--text2); font-family:'DM Mono';">${fmt(r.fechaHora)}</td>
      </tr>
    `).join('');
  } catch (e) {
    console.error(e);
  }
}

function printReport() {
  window.print();
}
