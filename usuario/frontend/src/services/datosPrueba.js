// Datos de prueba para usar el panel sin el backend (por ejemplo, en una demo sin base de datos).
// Se activan con VITE_DATOS_PRUEBA=true en el archivo .env.
// Sin esa línea (o con false) se usa el backend real.
// Los cambios (crear, editar, activar, desactivar) se pierden al recargar la página.

export const DATOS_PRUEBA = import.meta.env.VITE_DATOS_PRUEBA === "true";

const esperar = (ms = 350) => new Promise((r) => setTimeout(r, ms));

const NOMBRES = [
  ["Lucía Fernández", "CLIENTE"], ["Carlos Quispe", "MECANICO"], ["María Torres", "CLIENTE"],
  ["José Huamán", "CLIENTE"], ["Andrea Salazar", "CLIENTE"], ["Miguel Rojas", "MECANICO"],
  ["Valeria Castillo", "CLIENTE"], ["Diego Mendoza", "CLIENTE"], ["Rosa Paredes", "CLIENTE"],
  ["Luis Chávez", "MECANICO"], ["Camila Flores", "CLIENTE"], ["Jorge Ramírez", "CLIENTE"],
  ["Daniela Vargas", "CLIENTE"], ["Renzo Gutiérrez", "MECANICO"], ["Paola Medina", "CLIENTE"],
  ["Fernando Ríos", "CLIENTE"], ["Gabriela Núñez", "CLIENTE"], ["Kevin Espinoza", "MECANICO"],
  ["Sofía Cárdenas", "CLIENTE"], ["Alonso Vega", "CLIENTE"], ["Patricia León", "CLIENTE"],
  ["Ricardo Soto", "CLIENTE"],
];

const sinTildes = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "");

let usuarios = [
  {
    id: 1,
    nombre: "Administrador AutoSmart",
    correo: "admin@autosmart.com",
    rol: "ADMIN",
    activo: true,
    creadoEn: "2026-09-01T10:00:00",
  },
  ...NOMBRES.map(([nombre, rol], i) => {
    const [n, a] = sinTildes(nombre).toLowerCase().split(" ");
    const dia = new Date(2026, 8, 10 + i, 9 + (i % 8), (i * 7) % 60);
    return {
      id: i + 2,
      nombre,
      correo: `${n}.${a}@correo.com`,
      rol,
      activo: ![4, 11, 16].includes(i),
      creadoEn: dia.toISOString(),
    };
  }),
];
let siguienteId = usuarios.length + 1;

// Imita la respuesta paginada de Spring: { content, totalElements, totalPages, number, size }
export async function listarUsuarios({ rol = "", q = "", pagina = 0, tamano = 10 }) {
  await esperar();
  const texto = sinTildes(q.trim().toLowerCase());
  const filtrados = usuarios
    .filter((u) => !rol || u.rol === rol)
    .filter((u) => !texto || sinTildes(`${u.nombre} ${u.correo}`.toLowerCase()).includes(texto))
    .sort((a, b) => b.creadoEn.localeCompare(a.creadoEn));
  return {
    content: filtrados.slice(pagina * tamano, pagina * tamano + tamano),
    totalElements: filtrados.length,
    totalPages: Math.max(1, Math.ceil(filtrados.length / tamano)),
    number: pagina,
    size: tamano,
  };
}

// Contraseña temporal como la genera el backend: 10 caracteres con mayúscula, minúscula y dígito
function passwordTemporal() {
  const may = "ABCDEFGHJKLMNPQRSTUVWXYZ", min = "abcdefghijkmnpqrstuvwxyz", num = "23456789";
  const azar = (s) => s[Math.floor(Math.random() * s.length)];
  const letras = [azar(may), azar(min), azar(num)];
  while (letras.length < 10) letras.push(azar(may + min + num));
  return letras.sort(() => Math.random() - 0.5).join("");
}

export async function crearMecanico({ nombre, correo }) {
  await esperar(500);
  if (usuarios.some((u) => u.correo.toLowerCase() === correo.toLowerCase())) {
    throw new Error("El correo ya se encuentra registrado.");
  }
  const nuevo = {
    id: siguienteId++,
    nombre,
    correo,
    rol: "MECANICO",
    activo: true,
    creadoEn: new Date().toISOString(),
  };
  usuarios = [nuevo, ...usuarios];
  return { ...nuevo, passwordTemporal: passwordTemporal() };
}

export async function cambiarEstadoUsuario(id, activo) {
  await esperar(300);
  const usuario = usuarios.find((u) => u.id === id);
  if (!usuario) throw new Error("El usuario ya no existe.");
  usuario.activo = activo;
  return { ...usuario };
}

/* ---------- Servicios (catálogo) ---------- */

let servicios = [
  ["Cambio de aceite y filtro", "Aceite de motor según especificación del fabricante y cambio de filtro.", 120, 45, true, "PREVENTIVO"],
  ["Alineamiento y balanceo", "Ajuste de ángulos de dirección y balanceo de las cuatro ruedas.", 90, 60, true, "PREVENTIVO"],
  ["Revisión de frenos", "Inspección de pastillas, discos, líquido y mangueras de freno.", 80, 60, true, "PREVENTIVO"],
  ["Diagnóstico con escáner", "Lectura de códigos de falla de la computadora del vehículo.", 70, 40, true, "CORRECTIVO"],
  ["Revisión de batería", "Prueba de carga, bornes y sistema de arranque.", 30, 20, true, "PREVENTIVO"],
  ["Cambio de pastillas de freno", "Reemplazo de pastillas delanteras o posteriores.", 150, 90, true, "CORRECTIVO"],
  ["Afinamiento de motor", "Bujías, filtros de aire y combustible, limpieza de inyectores.", 220, 120, true, "PREVENTIVO"],
  ["Cambio de refrigerante", "Drenado y llenado del sistema de enfriamiento.", 95, 45, true, "PREVENTIVO"],
  ["Revisión de suspensión", "Inspección de amortiguadores, rótulas y terminales.", 110, 75, false, "CORRECTIVO"],
  ["Lavado de motor", "Limpieza exterior del compartimiento del motor.", 50, 40, false, "PREVENTIVO"],
].map(([nombre, descripcion, precioBase, duracionMinutos, activo, categoria], i) => ({
  id: i + 1,
  nombre,
  descripcion,
  precioBase,
  duracionMinutos,
  activo,
  categoria,
}));
let siguienteServicio = servicios.length + 1;

const mismoNombre = (a, b) => sinTildes(a.trim().toLowerCase()) === sinTildes(b.trim().toLowerCase());

export async function listarServicios() {
  await esperar();
  return servicios.map((s) => ({ ...s }));
}

export async function crearServicio(datos) {
  await esperar(450);
  if (servicios.some((s) => mismoNombre(s.nombre, datos.nombre))) {
    throw new Error("Ya existe un servicio con ese nombre.");
  }
  const nuevo = { id: siguienteServicio++, activo: true, ...datos };
  servicios = [...servicios, nuevo];
  return { ...nuevo };
}

export async function editarServicio(id, datos) {
  await esperar(450);
  if (servicios.some((s) => s.id !== id && mismoNombre(s.nombre, datos.nombre))) {
    throw new Error("Ya existe un servicio con ese nombre.");
  }
  const servicio = servicios.find((s) => s.id === id);
  if (!servicio) throw new Error("El servicio ya no existe.");
  Object.assign(servicio, datos);
  return { ...servicio };
}

export async function cambiarEstadoServicio(id, activo) {
  await esperar(300);
  const servicio = servicios.find((s) => s.id === id);
  if (!servicio) throw new Error("El servicio ya no existe.");
  servicio.activo = activo;
  return { ...servicio };
}

/* ---------- Recuperación de contraseña ---------- */

// Código fijo para probar la pantalla "Nueva contraseña" sin backend
export const CODIGO_PRUEBA = "123456";

export async function solicitarCodigo() {
  await esperar(600);
  return {};
}

export async function restablecerPassword(correo, codigo) {
  await esperar(600);
  if (codigo !== CODIGO_PRUEBA) throw new Error("El código no es válido o ya venció.");
  return {};
}

/* ---------- Horarios del mecánico ---------- */

let horarios = [
  { idHorario: 1, diaSemana: 1, horaInicio: "08:00", horaFin: "12:00" },
  { idHorario: 2, diaSemana: 1, horaInicio: "14:00", horaFin: "18:00" },
  { idHorario: 3, diaSemana: 3, horaInicio: "09:00", horaFin: "17:00" },
  { idHorario: 4, diaSemana: 5, horaInicio: "08:00", horaFin: "13:00" },
];
let siguienteHorario = 5;

function validarHorario({ diaSemana, horaInicio, horaFin }, excluirId) {
  if (horaFin <= horaInicio) throw new Error("La hora de fin debe ser posterior a la hora de inicio");
  const cruza = horarios.some(
    (h) => h.idHorario !== excluirId && h.diaSemana === diaSemana && horaInicio < h.horaFin && h.horaInicio < horaFin
  );
  if (cruza) throw new Error("El horario se cruza con otro rango registrado ese dia");
}

export async function listarHorarios() {
  await esperar();
  return horarios.map((h) => ({ ...h }));
}

export async function crearHorario(datos) {
  await esperar(300);
  validarHorario(datos, -1);
  const nuevo = { idHorario: siguienteHorario++, ...datos };
  horarios = [...horarios, nuevo];
  return { ...nuevo };
}

export async function editarHorario(id, datos) {
  await esperar(300);
  validarHorario(datos, id);
  const horario = horarios.find((h) => h.idHorario === id);
  if (!horario) throw new Error("Horario no encontrado");
  Object.assign(horario, datos);
  return { ...horario };
}

export async function eliminarHorario(id) {
  await esperar(250);
  horarios = horarios.filter((h) => h.idHorario !== id);
}

/* ---------- Fechas bloqueadas del mecánico ---------- */

let bloqueos = [];
let siguienteBloqueo = 1;

export async function listarBloqueos(desde) {
  await esperar();
  return bloqueos.filter((b) => !desde || b.fecha >= desde).map((b) => ({ ...b }));
}

export async function bloquearFecha({ fecha, motivo }) {
  await esperar(300);
  if (bloqueos.some((b) => b.fecha === fecha)) throw new Error("Esa fecha ya esta bloqueada");
  const nuevo = { idBloqueo: siguienteBloqueo++, fecha, motivo: motivo || null };
  bloqueos = [...bloqueos, nuevo];
  return { ...nuevo };
}

export async function desbloquearFecha(id) {
  await esperar(250);
  bloqueos = bloqueos.filter((b) => b.idBloqueo !== id);
}

/* ---------- Agenda del mecánico (citas de ejemplo alrededor de hoy) ---------- */

const dosCifras = (n) => String(n).padStart(2, "0");
const fechaRelativa = (dias) => {
  const d = new Date();
  d.setDate(d.getDate() + dias);
  return `${d.getFullYear()}-${dosCifras(d.getMonth() + 1)}-${dosCifras(d.getDate())}`;
};

const citasPrueba = [
  [0, "09:00", "10:00", "CONFIRMADA", "Alineamiento y balanceo", "Lucía Fernández", "ABC-123"],
  [0, "11:00", "11:45", "PENDIENTE", "Cambio de aceite y filtro", "José Huamán", "BCD-456"],
  [1, "08:00", "09:00", "CONFIRMADA", "Revisión de frenos", "María Torres", "CDE-789"],
  [1, "15:00", "15:40", "CANCELADA", "Diagnóstico con escáner", "Diego Mendoza", "DEF-321"],
  [2, "10:00", "11:30", "PENDIENTE", "Cambio de pastillas de freno", "Andrea Salazar", "EFG-654"],
  [3, "14:00", "16:00", "CONFIRMADA", "Afinamiento de motor", "Jorge Ramírez", "FGH-987"],
  [5, "09:30", "10:15", "RECHAZADA", "Cambio de refrigerante", "Paola Medina", "GHI-246"],
  [7, "08:00", "09:00", "PENDIENTE", "Revisión de frenos", "Camila Flores", "HIJ-135"],
  [0, "14:00", "15:00", "CONFIRMADA", "Revisión de suspensión", "Daniela Vargas", "JKL-482"],
  [0, "16:30", "17:10", "CANCELADA", "Revisión de batería", "Fernando Ríos", "KLM-517"],
  [0, "12:00", "13:00", "EN_PROCESO", "Cambio de amortiguadores", "Gabriela Núñez", "LMN-208"],
  [-1, "10:00", "11:00", "FINALIZADA", "Cambio de aceite y filtro", "Ricardo Paredes", "MNP-934"],
].map(([dias, horaInicio, horaFin, estado, servicio, cliente, placa], i) => ({
  idCita: i + 1,
  ticket: `AS-2026-${String(i + 101).padStart(5, "0")}`,
  fecha: fechaRelativa(dias),
  horaInicio,
  horaFin,
  estado,
  servicio,
  cliente,
  placa,
}));

export async function listarAgenda(desde, hasta) {
  await esperar();
  return citasPrueba.filter((c) => c.fecha >= desde && c.fecha <= hasta).map((c) => ({ ...c }));
}

/* ---------- Perfil profesional del mecánico ---------- */

let perfilMecanico = {
  idUsuario: 2,
  nombre: "Carlos Quispe",
  correo: "carlos@taller.com",
  aniosExperiencia: 0,
  especialidades: [],
};

export async function obtenerPerfilMecanico() {
  await esperar();
  return { ...perfilMecanico, especialidades: [...perfilMecanico.especialidades] };
}

export async function guardarPerfilMecanico({ aniosExperiencia, especialidades }) {
  await esperar(400);
  if (aniosExperiencia < 0 || aniosExperiencia > 60) throw new Error("Los anios de experiencia deben estar entre 0 y 60");
  if (especialidades.length > 10) throw new Error("Puedes registrar hasta 10 especialidades");
  const unicas = [...new Map(especialidades.map((e) => [e.trim().toLowerCase(), e.trim()])).values()];
  perfilMecanico = {
    ...perfilMecanico,
    aniosExperiencia,
    especialidades: unicas.map((e) => e.charAt(0).toUpperCase() + e.slice(1)).sort((a, b) => a.localeCompare(b)),
  };
  return obtenerPerfilMecanico();
}

/* ---------- Detalle de una cita (vehículo, teléfono y precio de ejemplo) ---------- */

const VEHICULOS = [
  ["Toyota", "Yaris", 2019, 58200],
  ["Hyundai", "Accent", 2021, 31450],
  ["Kia", "Rio", 2018, 74900],
  ["Nissan", "Sentra", 2020, 46300],
  ["Chevrolet", "Sail", 2017, 98120],
  ["Suzuki", "Swift", 2022, 18750],
];
const PRECIOS = [90, 120, 80, 70, 150, 220, 95, 80, 110, 30];

export async function obtenerCita(id) {
  await esperar(400);
  const i = citasPrueba.findIndex((c) => c.idCita === Number(id));
  if (i === -1) throw new Error("Cita no encontrada");
  const c = citasPrueba[i];
  const [marca, modelo, anio, kilometraje] = VEHICULOS[i % VEHICULOS.length];
  const [h1, m1] = c.horaInicio.split(":").map(Number);
  const [h2, m2] = c.horaFin.split(":").map(Number);
  return {
    ...c,
    precio: PRECIOS[i % PRECIOS.length],
    duracionMinutos: h2 * 60 + m2 - (h1 * 60 + m1),
    telefonoCliente: i % 4 === 3 ? null : `9${String(87654321 - i * 1111111).padStart(8, "0")}`,
    marca,
    modelo,
    anio,
    kilometraje,
  };
}

// Aceptar o rechazar (T36.1): solo citas pendientes
export async function responderCita(id, estado, motivo) {
  await esperar(450);
  const cita = citasPrueba.find((c) => c.idCita === Number(id));
  if (!cita) throw new Error("Cita no encontrada");
  if (cita.estado !== "PENDIENTE") throw new Error(`La cita ya no esta pendiente (estado: ${cita.estado})`);
  cita.estado = estado;
  if (motivo) cita.motivoRechazo = motivo;
  return { ...cita };
}

/* ---------- Órdenes de trabajo (T41.1) e inspección registrada desde la app (TEC3.2) ---------- */

const conEstado = (mensaje, status) => Object.assign(new Error(mensaje), { status });
const enHora = (dias, hora) => `${fechaRelativa(dias)}T${hora}:00`;

// Orden 1: cita en proceso de hoy (con checklist, deficiencias y fotos). Orden 2: cita finalizada de ayer.
const ordenesPrueba = [
  { idOrden: 1, idCita: 11, fechaInicio: enHora(0, "12:04"), fechaFin: null },
  { idOrden: 2, idCita: 12, fechaInicio: enHora(-1, "10:02"), fechaFin: enHora(-1, "10:58") },
];
let siguienteOrden = 3;

const datosOrden = (orden) => {
  const cita = citasPrueba.find((c) => c.idCita === orden.idCita);
  return { ...orden, ticket: cita.ticket, estado: cita.estado, servicio: cita.servicio, placa: cita.placa };
};

// Iniciar la atención (T41.1): solo citas confirmadas, una sola vez
export async function iniciarAtencion(id) {
  await esperar(450);
  const cita = citasPrueba.find((c) => c.idCita === Number(id));
  if (!cita) throw new Error("Cita no encontrada");
  if (ordenesPrueba.some((o) => o.idCita === cita.idCita)) throw conEstado("La atencion de esta cita ya fue iniciada", 409);
  if (cita.estado !== "CONFIRMADA") throw conEstado(`Solo se puede iniciar una cita CONFIRMADA (estado: ${cita.estado})`, 409);
  cita.estado = "EN_PROCESO";
  const d = new Date();
  const orden = {
    idOrden: siguienteOrden++,
    idCita: cita.idCita,
    fechaInicio: `${fechaRelativa(0)}T${dosCifras(d.getHours())}:${dosCifras(d.getMinutes())}:00`,
    fechaFin: null,
  };
  ordenesPrueba.push(orden);
  return datosOrden(orden);
}

export async function obtenerOrdenDeCita(idCita) {
  await esperar(300);
  if (!citasPrueba.some((c) => c.idCita === Number(idCita))) throw conEstado("Cita no encontrada", 404);
  const orden = ordenesPrueba.find((o) => o.idCita === Number(idCita));
  if (!orden) throw conEstado("La cita aun no tiene orden de trabajo", 404);
  return datosOrden(orden);
}

const checklistsPrueba = {
  1: {
    idChecklist: 1,
    fechaRegistro: enHora(0, "12:10"),
    observacionGeneral: "Ingresa con el tanque a 1/4. El cliente deja la llanta de repuesto y la gata en la maletera.",
    items: [
      { elemento: "LUCES", estado: "OK", observacion: null },
      { elemento: "FRENOS", estado: "OBSERVADO", observacion: "Pastillas delanteras al 20 %; chirrido al frenar." },
      { elemento: "NEUMATICOS", estado: "OK", observacion: null },
      { elemento: "FLUIDOS", estado: "OK", observacion: null },
      { elemento: "CARROCERIA", estado: "OBSERVADO", observacion: "Abolladura leve en la puerta trasera derecha." },
    ],
  },
  2: {
    idChecklist: 2,
    fechaRegistro: enHora(-1, "10:06"),
    observacionGeneral: null,
    items: ["LUCES", "FRENOS", "NEUMATICOS", "FLUIDOS", "CARROCERIA"].map((elemento) => ({ elemento, estado: "OK", observacion: null })),
  },
};

// Fotos de ejemplo (en el servidor real son JPG o PNG de la cámara del celular)
function fotoEjemplo(titulo, [color1, color2]) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600">
<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${color1}"/><stop offset="1" stop-color="${color2}"/></linearGradient></defs>
<rect width="800" height="600" fill="url(#g)"/>
<g fill="none" stroke="rgba(255,255,255,0.4)" stroke-width="14" stroke-linejoin="round">
<rect x="290" y="170" width="220" height="160" rx="26"/><path d="M350 170l18-30h64l18 30"/></g>
<circle cx="400" cy="252" r="46" fill="none" stroke="rgba(255,255,255,0.55)" stroke-width="14"/>
<text x="400" y="430" font-family="Arial, sans-serif" font-size="34" font-weight="700" fill="#fff" text-anchor="middle">${titulo}</text>
<text x="400" y="474" font-family="Arial, sans-serif" font-size="22" fill="rgba(255,255,255,0.75)" text-anchor="middle">Foto de ejemplo</text>
</svg>`;
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

const deficienciasPrueba = {
  1: [
    {
      idDeficiencia: 1,
      categoria: "FRENOS",
      severidad: "MODERADA",
      descripcion: "Pastillas de freno delanteras con 20 % de vida útil. Se recomienda cambiarlas en este servicio.",
      fechaRegistro: enHora(0, "12:20"),
      fotos: [{ idFoto: 3, src: fotoEjemplo("Pastillas delanteras", ["#7c2d12", "#ea580c"]) }],
    },
    {
      idDeficiencia: 2,
      categoria: "SUSPENSION",
      severidad: "CRITICA",
      descripcion:
        "Amortiguador delantero izquierdo con fuga de aceite y rebote excesivo; compromete la estabilidad al frenar.",
      fechaRegistro: enHora(0, "12:16"),
      fotos: [
        { idFoto: 1, src: fotoEjemplo("Amortiguador delantero izq.", ["#7f1d1d", "#dc2626"]) },
        { idFoto: 2, src: fotoEjemplo("Fuga de aceite", ["#1e293b", "#b91c1c"]) },
      ],
    },
    {
      idDeficiencia: 3,
      categoria: "CARROCERIA",
      severidad: "LEVE",
      descripcion: "Abolladura leve en la puerta trasera derecha. El cliente indica que ya la tenía.",
      fechaRegistro: enHora(0, "12:22"),
      fotos: [],
    },
  ],
  2: [
    {
      idDeficiencia: 4,
      categoria: "NEUMATICOS",
      severidad: "LEVE",
      descripcion: "Neumático trasero derecho con desgaste irregular; revisar el alineamiento en el próximo servicio.",
      fechaRegistro: enHora(-1, "10:12"),
      fotos: [{ idFoto: 4, src: fotoEjemplo("Neumático trasero der.", ["#0f172a", "#475569"]) }],
    },
  ],
};

const copia = (x) => JSON.parse(JSON.stringify(x));

// Diagnóstico técnico (T45.2): la orden 2 (finalizada) ya lo tiene; la 1 (en proceso) aún no
const diagnosticosPrueba = {
  2: {
    diagnostico:
      "Aceite degradado por kilometraje (9 800 km desde el último cambio). Se reemplazó el aceite y el filtro.\nNeumático trasero derecho con desgaste irregular: se recomienda alineamiento y balanceo en el próximo servicio.",
    fechaDiagnostico: enHora(-1, "10:40"),
  },
};

const ahoraEnLima = () => {
  const d = new Date(new Date().toLocaleString("en-US", { timeZone: "America/Lima" }));
  return `${fechaRelativa(0)}T${dosCifras(d.getHours())}:${dosCifras(d.getMinutes())}:${dosCifras(d.getSeconds())}`;
};

export async function obtenerDiagnostico(idOrden) {
  await esperar(250);
  const diagnostico = diagnosticosPrueba[Number(idOrden)];
  if (!diagnostico) throw conEstado("La orden aun no tiene diagnostico", 404);
  return { idOrden: Number(idOrden), ...diagnostico };
}

export async function guardarDiagnostico(idOrden, diagnostico) {
  await esperar(500);
  ordenEnProceso(idOrden);
  if (!diagnostico.trim()) throw conEstado("El diagnostico es obligatorio", 400);
  diagnosticosPrueba[Number(idOrden)] = { diagnostico: diagnostico.trim(), fechaDiagnostico: ahoraEnLima() };
  return { idOrden: Number(idOrden), ...diagnosticosPrueba[Number(idOrden)] };
}

// Como el backend: lo que cambia la orden solo se acepta con la cita EN_PROCESO
function ordenEnProceso(idOrden) {
  const orden = ordenesPrueba.find((o) => o.idOrden === Number(idOrden));
  if (!orden) throw conEstado("Orden de trabajo no encontrada", 404);
  const { estado } = datosOrden(orden);
  if (estado !== "EN_PROCESO") throw conEstado(`La orden no esta EN_PROCESO (estado: ${estado})`, 409);
  return orden;
}

/* ---------- Presupuesto (T46.2) ---------- */

const presupuestosPrueba = [
  {
    idPresupuesto: 1,
    idOrden: 2,
    estado: "APROBADO",
    descripcion: "Cambio de aceite y filtro según el kilometraje.",
    fechaCreacion: enHora(-1, "10:20"),
    fechaDecision: enHora(-1, "10:24"),
    items: [
      { tipo: "MANO_OBRA", descripcion: "Cambio de aceite y filtro", cantidad: 1, precioUnitario: 40, subtotal: 40 },
      { tipo: "REPUESTO", descripcion: "LUB-001 · Aceite de motor 10W-30", cantidad: 1, precioUnitario: 96, subtotal: 96 },
      { tipo: "REPUESTO", descripcion: "FIL-001 · Filtro de aceite", cantidad: 1, precioUnitario: 18.5, subtotal: 18.5 },
    ],
    total: 154.5,
  },
];
let siguientePresupuesto = 2;

export async function obtenerPresupuesto(idOrden) {
  await esperar(250);
  const p = [...presupuestosPrueba].reverse().find((x) => x.idOrden === Number(idOrden));
  if (!p) throw conEstado("La orden aun no tiene presupuesto", 404);
  return copia(p);
}

export async function enviarPresupuesto(idOrden, { descripcion, items }) {
  await esperar(500);
  const orden = ordenEnProceso(idOrden);
  if (!diagnosticosPrueba[orden.idOrden]) throw conEstado("Registra el diagnostico antes de generar el presupuesto", 409);
  const conSubtotal = items.map((i) => ({ ...i, subtotal: Math.round(i.precioUnitario * i.cantidad * 100) / 100 }));
  const p = {
    idPresupuesto: siguientePresupuesto++,
    idOrden: orden.idOrden,
    estado: "PENDIENTE",
    descripcion,
    fechaCreacion: ahoraEnLima(),
    fechaDecision: null,
    items: conSubtotal,
    total: Math.round(conSubtotal.reduce((s, i) => s + i.subtotal, 0) * 100) / 100,
  };
  presupuestosPrueba.push(p);
  citasPrueba.find((c) => c.idCita === orden.idCita).estado = "ESPERA_APROBACION";
  // En modo de prueba, el "cliente" aprueba el presupuesto a los 20 segundos
  setTimeout(() => {
    p.estado = "APROBADO";
    p.fechaDecision = ahoraEnLima();
    citasPrueba.find((c) => c.idCita === orden.idCita).estado = "EN_PROCESO";
  }, 20000);
  return copia(p);
}

// Materiales del almacén que ve el mecánico (GET /materials)
const materialesTaller = [
  [1, "FIL-001", "Filtro de aceite", "unidad", 18.5, 24, 10],
  [2, "LUB-001", "Aceite de motor 10W-30", "galón", 96, 15, 6],
  [3, "FRE-001", "Pastillas de freno delanteras", "juego", 120, 5, 4],
  [4, "FRE-003", "Disco de freno delantero", "unidad", 165, 4, 2],
  [5, "REP-003", "Amortiguador delantero", "unidad", 210, 4, 2],
  [6, "REP-004", "Rótula de suspensión", "unidad", 75, 1, 2],
  [7, "INS-001", "Limpiador de frenos en aerosol", "unidad", 19, 14, 6],
  [8, "FIL-004", "Filtro de cabina", "unidad", 35, 0, 4],
].map(([idMaterial, codigo, nombre, unidad, costoUnitario, stockActual, stockMinimo]) => ({
  idMaterial,
  codigo,
  nombre,
  unidad,
  costoUnitario,
  stockActual,
  stockMinimo,
}));

export async function listarMaterialesTaller() {
  await esperar(250);
  return materialesTaller
    .map((m) => ({ ...m, bajoStock: m.stockActual <= m.stockMinimo }))
    .sort((a, b) => a.nombre.localeCompare(b.nombre, "es"));
}

/* ---------- Solicitudes de repuestos (T50.2) ---------- */

const solicitudesPrueba = [
  {
    idSolicitud: 1,
    idOrden: 2,
    idMaterial: 2,
    cantidad: 1,
    motivo: "Cambio de aceite",
    estado: "APROBADA",
    solicitadoPor: "carlos@taller.com",
    fechaSolicitud: enHora(-1, "10:26"),
    atendidoPor: "admin@autosmart.com",
    fechaAtencion: enHora(-1, "10:30"),
    observacion: null,
  },
];
let siguienteSolicitud = 2;

const datosSolicitud = (s) => {
  const m = materialesTaller.find((x) => x.idMaterial === s.idMaterial);
  return { ...s, codigoMaterial: m.codigo, nombreMaterial: m.nombre, stockDisponible: m.stockActual };
};

export async function listarSolicitudes(idOrden) {
  await esperar(250);
  return solicitudesPrueba.filter((s) => s.idOrden === Number(idOrden)).map(datosSolicitud);
}

export async function solicitarRepuesto(idOrden, { idMaterial, cantidad, motivo }) {
  await esperar(450);
  const orden = ordenEnProceso(idOrden);
  const m = materialesTaller.find((x) => x.idMaterial === Number(idMaterial));
  if (!m) throw conEstado("Material no encontrado", 404);
  if (m.stockActual < cantidad) {
    throw conEstado(`Stock insuficiente de ${m.nombre}: disponible ${m.stockActual}, solicitado ${cantidad}`, 409);
  }
  const s = {
    idSolicitud: siguienteSolicitud++,
    idOrden: orden.idOrden,
    idMaterial: m.idMaterial,
    cantidad,
    motivo,
    estado: "PENDIENTE",
    solicitadoPor: "carlos@taller.com",
    fechaSolicitud: ahoraEnLima(),
    atendidoPor: null,
    fechaAtencion: null,
    observacion: null,
  };
  solicitudesPrueba.push(s);
  // En modo de prueba, el "administrador" la aprueba a los 20 segundos
  setTimeout(() => {
    if (s.estado !== "PENDIENTE" || m.stockActual < s.cantidad) return;
    s.estado = "APROBADA";
    s.atendidoPor = "admin@autosmart.com";
    s.fechaAtencion = ahoraEnLima();
    registrarSalidaPrueba(orden, m, s.cantidad, `Solicitud de repuesto #${s.idSolicitud}`);
  }, 20000);
  return datosSolicitud(s);
}

// Salida de stock hacia la orden (en T51.2 se listan como repuestos usados)
const consumosPrueba = [
  { idMovimiento: 1, idOrden: 2, idMaterial: 2, cantidad: 1, costoUnitario: 96, referencia: "Solicitud de repuesto #1", registradoPor: "admin@autosmart.com", fecha: enHora(-1, "10:30") },
  { idMovimiento: 2, idOrden: 2, idMaterial: 1, cantidad: 1, costoUnitario: 18.5, referencia: null, registradoPor: "carlos@taller.com", fecha: enHora(-1, "10:32") },
];
let siguienteMovimiento = 3;

function registrarSalidaPrueba(orden, m, cantidad, referencia) {
  m.stockActual -= cantidad;
  consumosPrueba.push({
    idMovimiento: siguienteMovimiento++,
    idOrden: orden.idOrden,
    idMaterial: m.idMaterial,
    cantidad,
    costoUnitario: m.costoUnitario,
    referencia,
    registradoPor: "carlos@taller.com",
    fecha: ahoraEnLima(),
  });
}

export async function listarConsumos(idOrden) {
  await esperar(250);
  const consumos = consumosPrueba
    .filter((c) => c.idOrden === Number(idOrden))
    .map((c) => ({ ...c, tipo: "SALIDA", codigoMaterial: materialesTaller.find((m) => m.idMaterial === c.idMaterial).codigo }));
  const costoRepuestos = Math.round(consumos.reduce((s, c) => s + c.costoUnitario * c.cantidad, 0) * 100) / 100;
  return { idOrden: Number(idOrden), costoRepuestos, consumos };
}

export async function registrarConsumo(idOrden, { idMaterial, cantidad, referencia }) {
  await esperar(450);
  const orden = ordenEnProceso(idOrden);
  const m = materialesTaller.find((x) => x.idMaterial === Number(idMaterial));
  if (!m) throw conEstado("Material no encontrado", 404);
  if (m.stockActual < cantidad) {
    throw conEstado(`Stock insuficiente de ${m.nombre}: disponible ${m.stockActual}, requerido ${cantidad}`, 409);
  }
  registrarSalidaPrueba(orden, m, cantidad, referencia);
  return listarConsumos(idOrden);
}

/* ---------- Completar el mantenimiento (T52.2) ---------- */

export async function completarMantenimiento(idOrden) {
  await esperar(500);
  const orden = ordenEnProceso(idOrden);
  if (!diagnosticosPrueba[orden.idOrden]) throw conEstado("Registra el diagnostico antes de finalizar la orden", 409);
  const tareas = tareasPrueba[orden.idOrden] ?? [];
  if (!tareas.length) throw conEstado("Registra al menos una tarea antes de finalizar la orden", 409);
  const pendientes = tareas.filter((t) => !t.completada).length;
  if (pendientes) throw conEstado(`Faltan ${pendientes} de ${tareas.length} tareas por completar`, 409);
  if (solicitudesPrueba.some((s) => s.idOrden === orden.idOrden && s.estado === "PENDIENTE")) {
    throw conEstado("Hay solicitudes de repuestos pendientes de atencion", 409);
  }
  orden.fechaFin = ahoraEnLima();
  const cita = citasPrueba.find((c) => c.idCita === orden.idCita);
  cita.estado = "FINALIZADA";
  const { costoRepuestos } = await listarConsumos(orden.idOrden);
  return { ...datosOrden(orden), costoRepuestos };
}

/* ---------- Tareas de la orden (T49.2) ---------- */

const tareasPrueba = {
  1: [
    { idTarea: 1, descripcion: "Levantar el vehículo y revisar la suspensión delantera", completada: true, fechaCreacion: enHora(0, "12:25"), fechaCompletada: enHora(0, "12:38") },
    { idTarea: 2, descripcion: "Cambiar amortiguadores delanteros", completada: false, fechaCreacion: enHora(0, "12:26"), fechaCompletada: null },
    { idTarea: 3, descripcion: "Prueba de manejo", completada: false, fechaCreacion: enHora(0, "12:26"), fechaCompletada: null },
  ],
  2: [
    { idTarea: 4, descripcion: "Drenar aceite usado", completada: true, fechaCreacion: enHora(-1, "10:15"), fechaCompletada: enHora(-1, "10:25") },
    { idTarea: 5, descripcion: "Cambiar filtro y aceite 10W-30", completada: true, fechaCreacion: enHora(-1, "10:15"), fechaCompletada: enHora(-1, "10:45") },
  ],
};
let siguienteTarea = 6;

function resumenTareas(idOrden) {
  const tareas = tareasPrueba[Number(idOrden)] ?? [];
  const completadas = tareas.filter((t) => t.completada).length;
  return {
    idOrden: Number(idOrden),
    totalTareas: tareas.length,
    completadas,
    porcentajeAvance: tareas.length ? Math.round((completadas * 100) / tareas.length) : 0,
    tareas: copia(tareas),
  };
}

export async function listarTareas(idOrden) {
  await esperar(250);
  return resumenTareas(idOrden);
}

export async function agregarTarea(idOrden, descripcion) {
  await esperar(350);
  ordenEnProceso(idOrden);
  const lista = (tareasPrueba[Number(idOrden)] ??= []);
  lista.push({ idTarea: siguienteTarea++, descripcion, completada: false, fechaCreacion: ahoraEnLima(), fechaCompletada: null });
  return resumenTareas(idOrden);
}

export async function marcarTarea(idOrden, idTarea, completada) {
  await esperar(300);
  ordenEnProceso(idOrden);
  const tarea = (tareasPrueba[Number(idOrden)] ?? []).find((t) => t.idTarea === Number(idTarea));
  if (!tarea) throw conEstado("Tarea no encontrada en esta orden", 404);
  tarea.completada = completada;
  tarea.fechaCompletada = completada ? ahoraEnLima() : null;
  return resumenTareas(idOrden);
}

export async function obtenerChecklist(idOrden) {
  await esperar(250);
  const checklist = checklistsPrueba[Number(idOrden)];
  if (!checklist) throw conEstado("La orden aun no tiene checklist de recepcion", 404);
  return { ...copia(checklist), idOrden: Number(idOrden) };
}

export async function listarDeficiencias(idOrden) {
  await esperar(250);
  return copia(deficienciasPrueba[Number(idOrden)] ?? []);
}

/* ---------- Reservas del taller para el administrador (T38.2) ---------- */

const SERVICIOS_RESERVA = [
  ["Cambio de aceite y filtro", 120, 45],
  ["Alineamiento y balanceo", 90, 60],
  ["Revisión de frenos", 80, 60],
  ["Diagnóstico con escáner", 70, 40],
  ["Afinamiento de motor", 220, 120],
  ["Cambio de pastillas de freno", 150, 90],
];
const HORAS_RESERVA = ["08:00", "09:00", "10:30", "11:00", "14:00", "15:30", "16:00"];
const PLACAS = ["ABC-123", "BCD-456", "CDE-789", "DEF-321", "EFG-654", "FGH-987", "GHI-246", "HIJ-135", "JKL-482"];

const sumarMinutos = (h, min) => {
  const [hh, mm] = h.split(":").map(Number);
  const t = hh * 60 + mm + min;
  return `${dosCifras(Math.floor(t / 60))}:${dosCifras(t % 60)}`;
};

const reservasPrueba = (() => {
  const mecanicos = usuarios.filter((u) => u.rol === "MECANICO");
  const clientes = usuarios.filter((u) => u.rol === "CLIENTE");
  const lista = [];
  for (let i = 0; i < 54; i++) {
    const dias = (i % 41) - 20; // de 20 días atrás a 20 días adelante
    const [servicio, precio, duracion] = SERVICIOS_RESERVA[i % SERVICIOS_RESERVA.length];
    const horaInicio = HORAS_RESERVA[(i * 3) % HORAS_RESERVA.length];
    const mecanico = mecanicos[(i * 7) % mecanicos.length];
    const pasada = dias < 0;
    const estado =
      i % 9 === 4 ? "CANCELADA" : i % 11 === 7 ? "RECHAZADA" : pasada || i % 3 === 0 ? "CONFIRMADA" : "PENDIENTE";
    lista.push({
      idCita: 500 + i,
      ticket: `AS-2026-${String(200 + i).padStart(5, "0")}`,
      fecha: fechaRelativa(dias),
      horaInicio,
      horaFin: sumarMinutos(horaInicio, duracion),
      estado,
      servicio,
      precio,
      cliente: clientes[(i * 5) % clientes.length].nombre,
      placa: PLACAS[i % PLACAS.length],
      idMecanico: mecanico.id,
      mecanico: mecanico.nombre,
    });
  }
  return lista;
})();

export async function listarReservas({ desde, hasta, estado, idMecanico, q, pagina, tamano }) {
  await esperar();
  const texto = sinTildes(q.trim().toLowerCase());
  const filtradas = reservasPrueba
    .filter((r) => (!desde || r.fecha >= desde) && (!hasta || r.fecha <= hasta))
    .filter((r) => !estado || r.estado === estado)
    .filter((r) => !idMecanico || r.idMecanico === Number(idMecanico))
    .filter((r) => !texto || sinTildes(`${r.ticket} ${r.cliente} ${r.placa}`.toLowerCase()).includes(texto))
    .sort((a, b) => a.fecha.localeCompare(b.fecha) || a.horaInicio.localeCompare(b.horaInicio));
  return {
    contenido: filtradas.slice(pagina * tamano, pagina * tamano + tamano).map((r) => ({ ...r })),
    pagina,
    tamano,
    totalElementos: filtradas.length,
    totalPaginas: Math.max(1, Math.ceil(filtradas.length / tamano)),
  };
}

/* ---------- Reasignar una reserva (T39.2) ---------- */

const ESPECIALIDADES_PRUEBA = ["Frenos", "Motor", "Suspensión", "Electricidad automotriz", "Mecánica general"];
const seCruzan = (a, b) => a.fecha === b.fecha && a.horaInicio < b.horaFin && b.horaInicio < a.horaFin;

function reservaReasignable(idCita) {
  const reserva = reservasPrueba.find((r) => r.idCita === Number(idCita));
  if (!reserva) throw new Error("Cita no encontrada");
  if (["CANCELADA", "RECHAZADA"].includes(reserva.estado)) throw new Error("La cita ya esta cancelada o rechazada");
  if (reserva.fecha < fechaRelativa(0)) throw new Error("La cita ya paso");
  return reserva;
}

// Libres = mecánicos activos, distintos del actual, sin otra reserva activa que se cruce
export async function listarMecanicosLibres(idCita) {
  await esperar(400);
  const reserva = reservaReasignable(idCita);
  return usuarios
    .filter((u) => u.rol === "MECANICO" && u.activo && u.id !== reserva.idMecanico)
    .filter(
      (u) =>
        !reservasPrueba.some(
          (r) => r.idMecanico === u.id && ["PENDIENTE", "CONFIRMADA"].includes(r.estado) && seCruzan(r, reserva)
        )
    )
    .map((u, i) => ({
      idUsuario: u.id,
      nombre: u.nombre,
      especialidades: [ESPECIALIDADES_PRUEBA[u.id % 5], ESPECIALIDADES_PRUEBA[(u.id + i + 2) % 5]].filter(
        (e, j, lista) => lista.indexOf(e) === j
      ),
    }))
    .sort((a, b) => a.nombre.localeCompare(b.nombre, "es"));
}

export async function reasignarReserva(idCita, idMecanico) {
  await esperar(450);
  const reserva = reservaReasignable(idCita);
  const nuevo = usuarios.find((u) => u.id === idMecanico && u.rol === "MECANICO");
  if (!nuevo) throw new Error("Mecanico no encontrado");
  if (nuevo.id === reserva.idMecanico) throw new Error("La cita ya esta asignada a ese mecanico");
  Object.assign(reserva, { idMecanico: nuevo.id, mecanico: nuevo.nombre, estado: "PENDIENTE" });
  return { ...reserva };
}

/* ---------- Horario del taller y feriados (T40.2) ---------- */

let horarioTaller = [1, 2, 3, 4, 5, 6, 7].map((dia) => ({
  diaSemana: dia,
  abierto: dia <= 6,
  horaApertura: "08:00",
  horaCierre: dia === 6 ? "13:00" : "18:00",
}));

let feriados = [
  { idFeriado: 1, fecha: "2026-10-08", descripcion: "Combate de Angamos" },
  { idFeriado: 2, fecha: "2026-11-01", descripcion: "Día de Todos los Santos" },
  { idFeriado: 3, fecha: "2026-12-08", descripcion: "Inmaculada Concepción" },
  { idFeriado: 4, fecha: "2026-12-25", descripcion: "Navidad" },
];
let siguienteFeriado = 5;

export async function obtenerHorarioTaller() {
  await esperar();
  return horarioTaller.map((d) => ({ ...d }));
}

export async function guardarHorarioTaller(dias) {
  await esperar(450);
  if (dias.length !== 7) throw new Error("Debes enviar los 7 dias de la semana");
  const malo = dias.find((d) => d.abierto && d.horaApertura >= d.horaCierre);
  if (malo) throw new Error(`La hora de apertura debe ser anterior a la de cierre (dia ${malo.diaSemana})`);
  horarioTaller = dias.map((d) => ({ ...d }));
  return obtenerHorarioTaller();
}

export async function listarFeriados(anio) {
  await esperar();
  return feriados.filter((f) => f.fecha.startsWith(String(anio))).map((f) => ({ ...f }));
}

export async function crearFeriado({ fecha, descripcion }) {
  await esperar(350);
  if (feriados.some((f) => f.fecha === fecha)) throw new Error("Ya existe un feriado en esa fecha");
  const nuevo = { idFeriado: siguienteFeriado++, fecha, descripcion };
  feriados = [...feriados, nuevo];
  return { ...nuevo };
}

export async function eliminarFeriado(id) {
  await esperar(250);
  feriados = feriados.filter((f) => f.idFeriado !== id);
}

/* ---------- Inventario del almacén (T57.2, contrato de T57.1) ---------- */

const mismoCodigo = (a, b) => a.trim().toUpperCase() === b.trim().toUpperCase();
const copiaMaterial = (m) => ({ ...m, bajoStock: m.stockActual <= m.stockMinimo });
const conflicto = (codigo) =>
  Object.assign(new Error(`Ya existe un material con el codigo ${codigo.trim().toUpperCase()}`), { status: 409 });

// [código, nombre, unidad, costo, stock, mínimo]
let materiales = [
  ["FIL-001", "Filtro de aceite", "unidad", 18.5, 24, 10],
  ["FIL-002", "Filtro de aire", "unidad", 32, 6, 8],
  ["FIL-003", "Filtro de combustible", "unidad", 28.9, 12, 5],
  ["FIL-004", "Filtro de cabina", "unidad", 35, 0, 4],
  ["LUB-001", "Aceite de motor 10W-30 sintético", "galón", 96, 15, 6],
  ["LUB-002", "Aceite de motor 20W-50 mineral", "galón", 72, 4, 6],
  ["LUB-003", "Líquido de frenos DOT 4", "litro", 24, 9, 4],
  ["LUB-004", "Refrigerante verde", "galón", 38, 7, 3],
  ["FRE-001", "Pastillas de freno delanteras", "juego", 120, 5, 4],
  ["FRE-002", "Pastillas de freno traseras", "juego", 98, 2, 3],
  ["FRE-003", "Disco de freno delantero", "unidad", 165, 4, 2],
  ["REP-001", "Bujía de iridio", "unidad", 42, 32, 16],
  ["REP-002", "Kit de distribución", "kit", 285, 3, 2],
  ["REP-003", "Amortiguador delantero", "unidad", 210, 4, 2],
  ["REP-004", "Rótula de suspensión", "unidad", 75, 1, 2],
  ["ELE-001", "Batería 12 V 65 Ah", "unidad", 420, 3, 2],
  ["ELE-002", "Foco H4 halógeno", "unidad", 16, 20, 10],
  ["ELE-003", "Cable automotriz 12 AWG", "metro", 3.5, 40, 15],
  ["NEU-001", "Neumático 185/65 R15", "unidad", 260, 8, 4],
  ["INS-001", "Limpiador de frenos en aerosol", "unidad", 19, 14, 6],
  ["INS-002", "Trapo industrial (bolsa de 1 kg)", "unidad", 9.5, 30, 10],
].map(([codigo, nombre, unidad, costoUnitario, stockActual, stockMinimo], i) => ({
  idMaterial: i + 1,
  codigo,
  nombre,
  unidad,
  costoUnitario,
  stockActual,
  stockMinimo,
}));
let siguienteMaterial = materiales.length + 1;

// Kardex: cada material empieza con una entrada de inventario inicial (T58.2)
const ahoraLima = () =>
  new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Lima", dateStyle: "short", timeStyle: "medium" })
    .format(new Date())
    .replace(" ", "T");
const movimientos = materiales.map((m, i) => ({
  idMovimiento: i + 1,
  idMaterial: m.idMaterial,
  codigoMaterial: m.codigo,
  tipo: "ENTRADA",
  cantidad: m.stockActual,
  costoUnitario: m.costoUnitario,
  stockResultante: m.stockActual,
  referencia: "Inventario inicial",
  idOrden: null,
  registradoPor: "admin@autosmart.com",
  fecha: `${fechaRelativa(-7)}T08:00:00`,
}));
let siguienteMovimientoAlmacen = movimientos.length + 1;

export async function registrarIngreso(idMaterial, { cantidad, costoUnitario, referencia }) {
  await esperar(450);
  const m = materiales.find((x) => x.idMaterial === Number(idMaterial));
  if (!m) throw Object.assign(new Error("Material no encontrado"), { status: 404 });
  if (!Number.isInteger(cantidad) || cantidad < 1) throw Object.assign(new Error("La cantidad debe ser al menos 1"), { status: 400 });
  if (costoUnitario !== undefined) m.costoUnitario = Math.round(costoUnitario * 100) / 100;
  m.stockActual += cantidad;
  const mov = {
    idMovimiento: siguienteMovimientoAlmacen++,
    idMaterial: m.idMaterial,
    codigoMaterial: m.codigo,
    tipo: "ENTRADA",
    cantidad,
    costoUnitario: m.costoUnitario,
    stockResultante: m.stockActual,
    referencia,
    idOrden: null,
    registradoPor: "admin@autosmart.com",
    fecha: ahoraLima(),
  };
  movimientos.push(mov);
  return { ...mov };
}

// Solicitudes de repuestos de los mecánicos (T59.2). Las órdenes 7 y 8 están activas; la 3 ya se finalizó.
const ordenesActivas = [7, 8];
const solicitudesAlmacen = [
  [1, 7, 14, 2, "Cambio de amortiguadores aprobado por el cliente", "carlos@taller.com", "09:12"],
  [2, 7, 9, 1, "Pastillas al 20 %", "carlos@taller.com", "09:15"],
  [3, 8, 5, 2, null, "ana@taller.com", "10:40"],
  [4, 3, 1, 1, "Cambio de aceite", "ana@taller.com", "08:05"],
].map(([idSolicitud, idOrden, idMaterial, cantidad, motivo, solicitadoPor, hora]) => ({
  idSolicitud,
  idOrden,
  idMaterial,
  cantidad,
  motivo,
  estado: idSolicitud === 4 ? "APROBADA" : "PENDIENTE",
  solicitadoPor,
  fechaSolicitud: `${fechaRelativa(idSolicitud === 4 ? -1 : 0)}T${hora}:00`,
  atendidoPor: idSolicitud === 4 ? "admin@autosmart.com" : null,
  fechaAtencion: idSolicitud === 4 ? `${fechaRelativa(-1)}T08:20:00` : null,
  observacion: null,
}));

const datosSolicitudAlmacen = (s) => {
  const m = materiales.find((x) => x.idMaterial === s.idMaterial);
  return { ...s, codigoMaterial: m.codigo, nombreMaterial: m.nombre, stockDisponible: m.stockActual };
};

export async function listarSolicitudesAlmacen(estado) {
  await esperar();
  const lista = estado ? solicitudesAlmacen.filter((s) => s.estado === estado) : [...solicitudesAlmacen];
  lista.sort((a, b) => (estado ? 1 : -1) * a.fechaSolicitud.localeCompare(b.fechaSolicitud));
  return lista.map(datosSolicitudAlmacen);
}

export async function decidirSolicitud(id, { decision, observacion }) {
  await esperar(450);
  const s = solicitudesAlmacen.find((x) => x.idSolicitud === Number(id));
  if (!s) throw Object.assign(new Error("Solicitud no encontrada"), { status: 404 });
  if (s.estado !== "PENDIENTE") throw Object.assign(new Error(`La solicitud ya fue ${s.estado}`), { status: 409 });
  if (decision === "APROBADA") {
    if (!ordenesActivas.includes(s.idOrden)) {
      throw Object.assign(new Error("La orden de la solicitud ya no esta activa (estado: FINALIZADA)"), { status: 409 });
    }
    const m = materiales.find((x) => x.idMaterial === s.idMaterial);
    if (m.stockActual < s.cantidad) {
      throw Object.assign(new Error(`Stock insuficiente de ${m.nombre}: disponible ${m.stockActual}, requerido ${s.cantidad}`), { status: 409 });
    }
    m.stockActual -= s.cantidad;
    movimientos.push({
      idMovimiento: siguienteMovimientoAlmacen++,
      idMaterial: m.idMaterial,
      codigoMaterial: m.codigo,
      tipo: "SALIDA",
      cantidad: s.cantidad,
      costoUnitario: m.costoUnitario,
      stockResultante: m.stockActual,
      referencia: `Solicitud de repuesto #${s.idSolicitud}`,
      idOrden: s.idOrden,
      registradoPor: "admin@autosmart.com",
      fecha: ahoraLima(),
    });
  }
  Object.assign(s, { estado: decision, atendidoPor: "admin@autosmart.com", fechaAtencion: ahoraLima(), observacion });
  return datosSolicitudAlmacen(s);
}

export async function obtenerKardex(idMaterial) {
  await esperar(300);
  return movimientos.filter((x) => x.idMaterial === Number(idMaterial)).map((x) => ({ ...x }));
}

export async function listarMateriales() {
  await esperar();
  return materiales.map(copiaMaterial).sort((a, b) => a.nombre.localeCompare(b.nombre, "es"));
}

export async function crearMaterial(datos) {
  await esperar(450);
  if (materiales.some((m) => mismoCodigo(m.codigo, datos.codigo))) throw conflicto(datos.codigo);
  const nuevo = {
    idMaterial: siguienteMaterial++,
    ...datos,
    codigo: datos.codigo.trim().toUpperCase(),
    unidad: datos.unidad.trim().toLowerCase(),
    stockActual: 0,
  };
  materiales = [...materiales, nuevo];
  return copiaMaterial(nuevo);
}

export async function editarMaterial(id, datos) {
  await esperar(450);
  const material = materiales.find((m) => m.idMaterial === id);
  if (!material) throw Object.assign(new Error("Material no encontrado"), { status: 404 });
  if (materiales.some((m) => m.idMaterial !== id && mismoCodigo(m.codigo, datos.codigo))) throw conflicto(datos.codigo);
  Object.assign(material, datos, { codigo: datos.codigo.trim().toUpperCase(), unidad: datos.unidad.trim().toLowerCase() });
  return copiaMaterial(material);
}

/* ---------- Calificaciones del mecánico (T67.2, contrato de T67.1) ---------- */

const RESENAS_PRUEBA = [
  [5, "Lucía F.", "Muy puntual y me explicó todo lo que le hizo al auto. ¡Recomendado!"],
  [4, "José H.", "Buen trabajo con los frenos, solo demoró un poco más de lo previsto."],
  [5, "Andrea S.", null],
  [3, "Diego M.", "El servicio estuvo bien, pero no me avisaron cuando el auto estuvo listo."],
  [5, "Camila F.", "Excelente atención, el auto quedó como nuevo."],
  [5, "Rosa P.", null],
  [2, "Jorge R.", null],
  [4, "Paola M.", "Amable y ordenado. Volvería."],
];

const calificacionesMecanico = RESENAS_PRUEBA.map(([estrellas, cliente, comentario], i) => ({
  idCalificacion: 40 - i,
  ticket: `AS-${fechaRelativa(-(i * 4 + 1)).replaceAll("-", "").slice(2)}-${String(14 + i * 3).padStart(4, "0")}`,
  cliente,
  estrellas,
  comentario,
  fecha: `${fechaRelativa(-(i * 4 + 1))}T${dosCifras(9 + (i % 8))}:${dosCifras((i * 13) % 60)}:00`,
}));

export async function obtenerMisCalificaciones() {
  await esperar();
  const total = calificacionesMecanico.length;
  const suma = calificacionesMecanico.reduce((s, c) => s + c.estrellas, 0);
  return {
    promedio: total ? Math.round((suma / total) * 10) / 10 : null,
    total,
    calificaciones: copia(calificacionesMecanico),
  };
}

/* ---------- Historial de servicios del mecánico (T68.2, contrato de T68.1) ---------- */

// La primera es la orden 2 (cita 12, finalizada ayer): se puede abrir en solo lectura
const historialMecanico = [
  [2, 12, "AS-2026-00112", "Cambio de aceite y filtro", "MNP-934", "Ricardo Paredes", -1, "10:02", 56, "FINALIZADA", null],
  [31, 131, "AS-2026-00091", "Revisión de frenos", "BCD-456", "José Huamán", -6, "09:00", 80, "PAGADO", 4],
  [29, 129, "AS-2026-00087", "Afinamiento de motor", "CDE-789", "Andrea Salazar", -12, "14:00", 140, "PAGADO", 5],
  [27, 127, "AS-2026-00085", "Revisión de suspensión", "DEF-321", "Camila Flores", -25, "11:00", 95, "PAGADO", 3],
  [24, 124, "AS-2026-00080", "Cambio de aceite y filtro", "ABC-123", "Lucía Fernández", -48, "08:30", 45, "PAGADO", 5],
  [20, 120, "AS-2026-00072", "Alineamiento y balanceo", "JKL-482", "Daniela Vargas", -95, "15:00", 70, "PAGADO", null],
].map(([idOrden, idCita, ticket, servicio, placa, cliente, dias, hora, minutos, estado, estrellas]) => {
  const [h, m] = hora.split(":").map(Number);
  const fin = h * 60 + m + minutos;
  return {
    idOrden,
    idCita,
    ticket,
    servicio,
    placa,
    cliente,
    fechaInicio: enHora(dias, hora),
    fechaFin: enHora(dias, `${dosCifras(Math.floor(fin / 60))}:${dosCifras(fin % 60)}`),
    estado,
    estrellas,
  };
});

export async function listarHistorialMecanico({ desde, hasta }) {
  await esperar();
  if (desde && hasta && desde > hasta) throw conEstado("La fecha 'desde' no puede ser posterior a 'hasta'", 400);
  const fin = hasta || fechaRelativa(0);
  return copia(historialMecanico.filter((o) => (!desde || o.fechaFin.slice(0, 10) >= desde) && o.fechaFin.slice(0, 10) <= fin));
}

/* ---------- Indicadores del taller (T73.2, contrato de T73.1) ---------- */

export async function obtenerMetricas(mes) {
  await esperar(250);
  const actual = fechaRelativa(0).slice(0, 7);
  const vacio = mes > actual || mes < "2026-08";
  const k = vacio ? 0 : mes === actual ? 1 : 0.55 + ((Number(mes.slice(5)) * 37) % 40) / 100;
  const n = (x) => Math.round(x * k);
  const s = (x) => Math.round(x * k * 100) / 100;
  return {
    mes,
    totalCitas: n(42),
    citasPorEstado: {
      PENDIENTE: n(4),
      CONFIRMADA: n(6),
      RECHAZADA: n(1),
      CANCELADA: n(3),
      EN_PROCESO: n(2),
      ESPERA_APROBACION: n(1),
      FINALIZADA: n(5),
      PAGADO: n(20),
    },
    ingresos: s(1820) + s(1090.5) + s(540),
    ingresosPorMetodo: { TARJETA: s(1820), YAPE_PLIN: s(1090.5), EFECTIVO: s(540) },
    tiempoPromedioMinutos: n(84),
    calificacionPromedio: vacio ? null : mes === actual ? 4.3 : 4.1,
    totalCalificaciones: vacio ? 0 : n(6),
  };
}

/* ---------- Pagos del taller (T70.2, contrato de T70.1) ---------- */

const pagosPrueba = [
  [12, 0, "11:20", "Lucía Fernández", "ABC-123", "AS-2026-00112", "TARJETA", "APROBADO", 185.5, null],
  [11, -1, "17:05", "Rosa Paredes", "GHI-246", "AS-2026-00108", "YAPE_PLIN", "APROBADO", 120, null],
  [10, -2, "09:40", "Diego Mendoza", "DEF-321", "AS-2026-00104", "TARJETA", "RECHAZADO", 240, null],
  [9, -2, "10:02", "Diego Mendoza", "DEF-321", "AS-2026-00104", "YAPE_PLIN", "APROBADO", 240, null],
  [8, -4, "16:30", "Andrea Salazar", "CDE-789", "AS-2026-00099", "EFECTIVO", "APROBADO", 140, "admin@autosmart.com"],
  [7, -6, "12:15", "Camila Flores", "HIJ-135", "AS-2026-00095", "TARJETA", "APROBADO", 95, null],
  [6, -9, "15:45", "José Huamán", "BCD-456", "AS-2026-00090", "EFECTIVO", "APROBADO", 230, "admin@autosmart.com"],
  [5, -12, "10:10", "Paola Medina", "GHI-246", "AS-2026-00086", "YAPE_PLIN", "APROBADO", 70, null],
  [4, -26, "14:00", "Jorge Ramírez", "FGH-987", "AS-2026-00078", "TARJETA", "APROBADO", 310, null],
  [3, -33, "09:30", "Daniela Vargas", "JKL-482", "AS-2026-00071", "TARJETA", "APROBADO", 150, null],
].map(([idPago, dias, hora, cliente, placa, ticket, metodo, estado, monto, registradoPor]) => ({
  idPago,
  codigo: `PAG-2026-${String(idPago).padStart(5, "0")}`,
  fecha: enHora(dias, hora),
  ticket,
  cliente,
  placa,
  metodo,
  estado,
  monto,
  registradoPor,
}));

export async function listarPagosAdmin({ desde, hasta, metodo, estado }) {
  await esperar();
  const hoy = fechaRelativa(0);
  const inicio = desde || `${hoy.slice(0, 8)}01`;
  const fin = hasta || hoy;
  if (inicio > fin) throw conEstado("La fecha 'desde' no puede ser posterior a 'hasta'", 400);
  const lista = pagosPrueba
    .filter((p) => p.fecha.slice(0, 10) >= inicio && p.fecha.slice(0, 10) <= fin)
    .filter((p) => (!metodo || p.metodo === metodo) && (!estado || p.estado === estado))
    .sort((a, b) => b.fecha.localeCompare(a.fecha));
  const total = lista.filter((p) => p.estado === "APROBADO").reduce((s, p) => s + p.monto, 0);
  return { desde: inicio, hasta: fin, cantidad: lista.length, totalAprobado: Math.round(total * 100) / 100, pagos: copia(lista) };
}

// PDF de una página con una línea de texto (para los archivos de ejemplo)
function pdfDeTexto(texto) {
  const limpio = texto.normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/[()\\]/g, "");
  const pdf = `%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 520 200]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj\n4 0 obj<</Length ${limpio.length + 30}>>stream\nBT /F1 11 Tf 20 100 Td (${limpio}) Tj ET\nendstream endobj\n5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF`;
  return new Blob([pdf], { type: "application/pdf" });
}

// Comprobante de ejemplo con el código del pago
export async function comprobantePrueba(pago) {
  await esperar(250);
  return pdfDeTexto(`Comprobante ${pago.codigo} - ${pago.cliente} - S/ ${pago.monto.toFixed(2)} (datos de prueba)`);
}

/* ---------- Historial por placa (T72.2) y cobro en efectivo (T69.2), contratos de T72.1 y T69.1 ---------- */

const vehiculosAdmin = {
  "MNP-934": {
    idVehiculo: 12, placa: "MNP-934", marca: "Nissan", modelo: "Sentra", anio: 2018, kilometraje: 87400,
    propietario: { nombre: "Ricardo Paredes", correo: "ricardo.paredes@correo.com" },
    mantenimientos: [
      { idOrden: 2, idCita: 12, ticket: "AS-2026-00112", estado: "FINALIZADA", servicio: "Cambio de aceite y filtro", mecanico: "Carlos Quispe", fechaInicio: enHora(-1, "10:02"), fechaFin: enHora(-1, "10:58"), diagnostico: "Aceite degradado y filtro saturado; se reemplazaron.", costoRepuestos: 114.5 },
      { idOrden: 14, idCita: 64, ticket: "AS-2026-00064", estado: "PAGADO", servicio: "Revisión de frenos", mecanico: "Miguel Rojas", fechaInicio: enHora(-75, "09:00"), fechaFin: enHora(-75, "10:15"), diagnostico: "Pastillas delanteras al 30 %; se recomienda cambio en 5000 km.", costoRepuestos: 0 },
    ],
  },
  "BCD-456": {
    idVehiculo: 7, placa: "BCD-456", marca: "Hyundai", modelo: "Accent", anio: 2017, kilometraje: 91200,
    propietario: { nombre: "José Huamán", correo: "jose.huaman@correo.com" },
    mantenimientos: [
      { idOrden: 31, idCita: 131, ticket: "AS-2026-00091", estado: "FINALIZADA", servicio: "Revisión de frenos", mecanico: "Carlos Quispe", fechaInicio: enHora(-6, "09:00"), fechaFin: enHora(-6, "10:20"), diagnostico: "Pastillas delanteras gastadas y disco rayado.", costoRepuestos: 150 },
    ],
  },
  "ABC-123": {
    idVehiculo: 3, placa: "ABC-123", marca: "Toyota", modelo: "Yaris", anio: 2019, kilometraje: 58200,
    propietario: { nombre: "Lucía Fernández", correo: "lucia.fernandez@correo.com" },
    mantenimientos: [],
  },
};

// Total que calcula el backend para cada orden (servicio + adicionales aprobados + repuestos)
const totalesOrden = { 2: 204.5, 31: 230 };

const porCobrarPrueba = [
  { idCita: 12, ticket: "AS-2026-00112", fecha: fechaRelativa(-1), horaInicio: "10:00", horaFin: "11:00", estado: "FINALIZADA", servicio: "Cambio de aceite y filtro", precio: 90, cliente: "Ricardo Paredes", placa: "MNP-934", idMecanico: 2, mecanico: "Carlos Quispe" },
  { idCita: 131, ticket: "AS-2026-00091", fecha: fechaRelativa(-6), horaInicio: "09:00", horaFin: "10:00", estado: "FINALIZADA", servicio: "Revisión de frenos", precio: 80, cliente: "José Huamán", placa: "BCD-456", idMecanico: 2, mecanico: "Carlos Quispe" },
];

export async function obtenerHistorialPlaca(placa) {
  await esperar();
  const v = vehiculosAdmin[placa.trim().toUpperCase()];
  if (!v) throw conEstado(`No existe un vehiculo con la placa ${placa.trim().toUpperCase()}`, 404);
  return { ...copia(v), totalMantenimientos: v.mantenimientos.length };
}

export async function listarPorCobrar() {
  await esperar();
  return porCobrarPrueba
    .filter((c) => c.estado === "FINALIZADA")
    .map((c) => ({ ...c, id: c.idCita }));
}

export async function registrarCobroEfectivo({ idOrden, monto, fecha, observacion }) {
  await esperar(450);
  const vehiculo = Object.values(vehiculosAdmin).find((v) => v.mantenimientos.some((m) => m.idOrden === idOrden));
  const mant = vehiculo?.mantenimientos.find((m) => m.idOrden === idOrden);
  if (!mant) throw conEstado("Orden de trabajo no encontrada", 404);
  if (mant.estado === "PAGADO") throw conEstado("La orden ya fue pagada", 409);
  const total = totalesOrden[idOrden] ?? 0;
  if (monto < total) throw conEstado(`El monto recibido es menor al total de la orden (S/ ${total.toFixed(2)})`, 400);
  mant.estado = "PAGADO";
  const cita = porCobrarPrueba.find((c) => c.idCita === mant.idCita);
  if (cita) cita.estado = "PAGADO";
  const idPago = Math.max(...pagosPrueba.map((p) => p.idPago)) + 1;
  const pago = {
    idPago,
    codigo: `PAG-2026-${String(idPago).padStart(5, "0")}`,
    fecha: fecha || enHora(0, "12:00"),
    ticket: mant.ticket,
    cliente: vehiculo.propietario.nombre,
    placa: vehiculo.placa,
    metodo: "EFECTIVO",
    estado: "APROBADO",
    monto: total,
    registradoPor: "admin@autosmart.com",
    observacion,
  };
  pagosPrueba.unshift(pago);
  return { ...pago, idOrden, motivoRechazo: null, comprobanteUrl: `/payments/${idPago}/receipt` };
}

/* ---------- Reportes en Excel y PDF (T74.2, contrato de T74.1) ---------- */

// Sin backend no se puede armar un .xlsx real: el "Excel" de prueba es un CSV que Excel abre igual
export async function reportePrueba(tipo, formato, filtros) {
  await esperar(400);
  const periodo = filtros.mes || `${filtros.desde || "inicio"}_${filtros.hasta || "hoy"}`;
  let filas;
  if (tipo === "pagos") {
    const { pagos } = await listarPagosAdmin(filtros);
    filas = [["Codigo", "Fecha", "Ticket", "Cliente", "Placa", "Metodo", "Estado", "Monto"], ...pagos.map((p) => [p.codigo, p.fecha, p.ticket, p.cliente, p.placa, p.metodo, p.estado, p.monto])];
  } else if (tipo === "metricas") {
    const m = await obtenerMetricas(filtros.mes || fechaRelativa(0).slice(0, 7));
    filas = [["Indicador", "Valor"], ["Citas", m.totalCitas], ["Ingresos (S/)", m.ingresos], ["Tiempo promedio (min)", m.tiempoPromedioMinutos], ["Calificacion promedio", m.calificacionPromedio ?? "-"]];
  } else {
    filas = [["Ticket", "Fecha", "Estado", "Cliente", "Placa"], ...reservasPrueba.filter((r) => (!filtros.desde || r.fecha >= filtros.desde) && (!filtros.hasta || r.fecha <= filtros.hasta) && (!filtros.estado || r.estado === filtros.estado)).map((r) => [r.ticket, r.fecha, r.estado, r.cliente, r.placa])];
  }
  if (formato === "pdf") {
    return { blob: pdfDeTexto(`Reporte de ${tipo} ${periodo}: ${filas.length - 1} filas (datos de prueba)`), nombre: `reporte-${tipo}-${periodo}.pdf` };
  }
  const csv = "\ufeff" + filas.map((f) => f.map((c) => `"${String(c).replaceAll('"', '""')}"`).join(";")).join("\r\n");
  return { blob: new Blob([csv], { type: "text/csv;charset=utf-8" }), nombre: `reporte-${tipo}-${periodo}.csv` };
}

/* ---------- Moderación de calificaciones (T71.2, contrato de T71.1) ---------- */

const calificacionesAdmin = [
  [41, 5, "Lucía Fernández", "Carlos Quispe", "Muy puntual y me explicó todo lo que le hizo al auto. ¡Recomendado!", -1],
  [40, 1, "Diego Mendoza", "Miguel Rojas", "Un idiota me atendió, son unos estafadores. No vuelvo más.", -2],
  [39, 4, "José Huamán", "Carlos Quispe", "Buen trabajo con los frenos, solo demoró un poco más de lo previsto.", -4],
  [38, 2, "Jorge Ramírez", "Luis Chávez", "Llámenme al 987 654 321 para reclamar, el auto sigue sonando.", -6],
  [37, 5, "Andrea Salazar", "Carlos Quispe", null, -8],
  [36, 3, "Camila Flores", "Miguel Rojas", "El servicio estuvo bien, pero no me avisaron cuando el auto estuvo listo.", -11],
  [35, 1, "Paola Medina", "Luis Chávez", "Visiten mitallerbarato.com, ahí sí atienden bien.", -15],
].map(([idCalificacion, estrellas, cliente, mecanico, comentario, dias], i) => ({
  idCalificacion,
  ticket: `AS-2026-${String(160 - i).padStart(5, "0")}`,
  cliente,
  mecanico,
  estrellas,
  comentario,
  fecha: enHora(dias, `${dosCifras(10 + i)}:${dosCifras((i * 17) % 60)}`),
  oculto: idCalificacion === 35,
  motivoOcultado: idCalificacion === 35 ? "Publicidad o spam" : null,
  ocultadoPor: idCalificacion === 35 ? "admin@autosmart.com" : null,
  fechaOcultado: idCalificacion === 35 ? enHora(-14, "09:30") : null,
}));

export async function listarCalificacionesAdmin(oculto) {
  await esperar();
  return copia(calificacionesAdmin.filter((c) => !oculto || String(c.oculto) === oculto));
}

export async function ocultarComentario(id, { motivo }) {
  await esperar(400);
  const c = calificacionesAdmin.find((x) => x.idCalificacion === Number(id));
  if (!c) throw conEstado("Calificación no encontrada", 404);
  if (c.oculto) throw conEstado("La calificación ya está oculta", 409);
  if (!motivo) throw conEstado("El motivo es obligatorio", 400);
  Object.assign(c, { oculto: true, motivoOcultado: motivo, ocultadoPor: "admin@autosmart.com", fechaOcultado: enHora(0, "12:00") });
  return copia(c);
}

/* ---------- Derivaciones del chatbot (T79.3, contrato de T79.1) ---------- */

const derivacionesPrueba = [
  { idDerivacion: 4, idConversacion: "c-4", fecha: enHora(0, "09:12"), usuario: "Lucía Fernández", contacto: "lucia.fernandez@correo.com", resumen: "USUARIO: ¿puedo cambiar la fecha de mi cita?\nBOT: Puedes reprogramarla desde Mis citas en la app.\nUSUARIO: y si es para hoy mismo?\nBOT: No entendí tu consulta. Puedo ayudarte con estas opciones:\nUSUARIO: quiero hablar con un asesor", estado: "PENDIENTE", nota: null, atendidoPor: null, fechaAtencion: null },
  { idDerivacion: 3, idConversacion: "c-3", fecha: enHora(0, "08:40"), usuario: "Visitante sin cuenta", contacto: "987 654 321", resumen: "USUARIO: hacen planchado y pintura?\nBOT: Estos son nuestros servicios:\n• Cambio de aceite – desde S/ 50.00\n• Revisión de frenos – desde S/ 80.00\nUSUARIO: hablar con un asesor", estado: "PENDIENTE", nota: null, atendidoPor: null, fechaAtencion: null },
  { idDerivacion: 2, idConversacion: "c-2", fecha: enHora(-2, "16:40"), usuario: "Visitante sin cuenta", contacto: null, resumen: "USUARIO: tienen servicio de grúa?\nBOT: No entendí tu consulta.\nUSUARIO: hablar con asesor", estado: "ATENDIDA", nota: "Volvió a escribir; se le indicó que no contamos con grúa.", atendidoPor: "admin@autosmart.com", fechaAtencion: enHora(-2, "17:05") },
];

export async function listarDerivaciones(estado) {
  await esperar();
  return copia(derivacionesPrueba.filter((d) => !estado || d.estado === estado));
}

export async function atenderDerivacion(id, { nota }) {
  await esperar(400);
  const d = derivacionesPrueba.find((x) => x.idDerivacion === Number(id));
  if (!d) throw conEstado("Derivación no encontrada", 404);
  if (d.estado === "ATENDIDA") throw conEstado("La derivación ya fue atendida", 409);
  Object.assign(d, { estado: "ATENDIDA", nota, atendidoPor: "admin@autosmart.com", fechaAtencion: enHora(0, "12:00") });
  return copia(d);
}

/* ---------- Base de conocimiento y respuestas del chatbot (T80.2, TEC4.1; contratos de T80.1 y T75.1) ---------- */

let faqPrueba = [
  [1, "¿Aceptan Yape o Plin?", "Sí. Puedes pagar con Yape o Plin desde la app, además de tarjeta o en efectivo en el taller.", "yape, plin, pago, pagar", true],
  [2, "¿Los trabajos tienen garantía?", "Sí. Todos nuestros servicios tienen 30 días de garantía en mano de obra.", "garantia, garantía, reclamo", true],
  [3, "¿Me avisan antes de hacer un trabajo adicional?", "Sí. Si el mecánico encuentra algo más, te enviamos un presupuesto a la app y solo se hace si lo apruebas.", "adicional, presupuesto, aprobar", true],
  [4, "¿Dónde están ubicados?", "Estamos en Av. Cascanueces 2221, Santa Anita, Lima.", "ubicacion, ubicación, direccion, dirección, donde", false],
].map(([idFaq, pregunta, respuesta, palabrasClave, activa]) => ({
  idFaq, pregunta, respuesta, palabrasClave, activa, fechaCreacion: enHora(-30, "10:00"), fechaActualizacion: activa ? null : enHora(-3, "16:20"),
}));
let siguienteFaq = 5;

export async function listarFaq() {
  await esperar();
  return copia(faqPrueba);
}

export async function crearFaq(datos) {
  await esperar(400);
  const faq = { idFaq: siguienteFaq++, ...datos, activa: true, fechaCreacion: enHora(0, "12:00"), fechaActualizacion: null };
  faqPrueba = [...faqPrueba, faq];
  return copia(faq);
}

export async function editarFaq(id, datos) {
  await esperar(400);
  const faq = faqPrueba.find((f) => f.idFaq === Number(id));
  if (!faq) throw conEstado("Pregunta frecuente no encontrada", 404);
  Object.assign(faq, datos, { fechaActualizacion: enHora(0, "12:00") });
  return copia(faq);
}

export async function desactivarFaq(id) {
  await esperar(400);
  const faq = faqPrueba.find((f) => f.idFaq === Number(id));
  if (!faq) throw conEstado("Pregunta frecuente no encontrada", 404);
  if (!faq.activa) throw conEstado("La pregunta frecuente ya está desactivada", 409);
  Object.assign(faq, { activa: false, fechaActualizacion: enHora(0, "12:00") });
  return copia(faq);
}

const OPCIONES_CHAT = ["Ver servicios", "Horario de atención", "Cotizar un servicio", "Preguntas frecuentes", "Hablar con un asesor"];

// Respuestas de ejemplo con las mismas intenciones que el chatbot real
export async function mensajeChatbot({ mensaje, idConversacion }) {
  await esperar(450);
  const texto = sinTildes(String(mensaje).toLowerCase());
  const id = idConversacion || `prueba-${Date.now()}`;
  const r = (intencion, respuesta, opciones = [], datos = null, requiereLogin = false) => ({ idConversacion: id, intencion, respuesta, opciones, datos, requiereLogin });
  if (/(hola|buen(os|as)|saludos)/.test(texto)) return r("saludo", "¡Hola! Soy el asistente de AutoSmart. ¿En qué te ayudo?", OPCIONES_CHAT);
  if (/preguntas frecuentes|faq/.test(texto)) return r("faq", "Estas son las preguntas más frecuentes. Toca una para ver la respuesta.", faqPrueba.filter((f) => f.activa).map((f) => f.pregunta));
  const faq = faqPrueba
    .filter((f) => f.activa)
    .map((f) => ({ f, puntos: sinTildes(f.pregunta.toLowerCase()) === texto ? 100 : String(f.palabrasClave ?? "").split(",").filter((p) => p.trim() && texto.includes(sinTildes(p.trim()))).length }))
    .filter((x) => x.puntos > 0)
    .sort((a, b) => b.puntos - a.puntos)[0];
  if (faq) return r("faq", faq.f.respuesta, ["Preguntas frecuentes", "Ver servicios"], { idFaq: faq.f.idFaq });
  if (/horario|hora|abren|atienden/.test(texto)) return r("horario", "Nuestro horario de atención:\nLunes a viernes: 08:00 a 18:00\nSábado: 08:00 a 13:00\nDomingo: cerrado", ["Agendar una cita"]);
  if (/precio|cotiz|cuanto (cuesta|sale|vale)|costo/.test(texto)) return r("cotizar", "Cambio de aceite y filtro cuesta entre S/ 90.00 y S/ 103.50. Es un precio referencial: el monto final depende del diagnóstico.", ["Agendar una cita", "Ver servicios"], { servicio: "Cambio de aceite y filtro", precioDesde: 90, precioHasta: 103.5, referencial: true });
  if (/servicio/.test(texto)) return r("servicios", "Estos son nuestros servicios:\n• Cambio de aceite y filtro – desde S/ 90.00 (45 min)\n• Revisión de frenos – desde S/ 80.00 (60 min)\n• Alineamiento y balanceo – desde S/ 70.00 (60 min)", ["Cotizar un servicio", "Agendar una cita"]);
  if (/estado|mi (auto|carro|vehiculo)|como va/.test(texto)) return r("consultar_estado", "Para consultar el estado de tu vehículo necesitas iniciar sesión.", ["Iniciar sesión"], null, true);
  if (/agendar|reservar|cita/.test(texto)) return r("agendar", "Para agendar una cita necesitas iniciar sesión.", ["Iniciar sesión"], null, true);
  if (/asesor|humano|persona/.test(texto)) return r("hablar_asesor", "Listo, registré tu solicitud. Un asesor te contactará en horario de atención.", [], { idDerivacion: 9 });
  return r("no_entendido", "No entendí tu consulta. Puedo ayudarte con estas opciones:", OPCIONES_CHAT);
}

/* ---------- Panel del cliente: perfil (CLI-01, contrato de T05.1) ---------- */

let perfilCliente = { idUsuario: 3, nombre: "Lucía Fernández", correo: "lucia.fernandez@correo.com", telefono: null, rol: "CLIENTE" };

export async function obtenerPerfilCliente() {
  await esperar();
  return { ...perfilCliente, telefono: perfilCliente.telefono ?? "" };
}

export async function guardarPerfilCliente({ nombre, telefono }) {
  await esperar(400);
  if (telefono && !/^\+?[0-9]{7,15}$/.test(telefono)) throw conEstado("El telefono debe tener entre 7 y 15 digitos", 400);
  perfilCliente = { ...perfilCliente, nombre, telefono: telefono || null };
  return { ...perfilCliente, telefono: perfilCliente.telefono ?? "" };
}

/* ---------- Vehículos del cliente (CLI-02) ---------- */

// Placas de otros clientes: registrarlas responde 409, igual que el backend
const PLACAS_OTROS_CLIENTES = ["BCD-456", "MNP-934", "DEF-321", "JKL-482", "GHI-246", "CDE-789", "HIJ-135", "FGH-987"];
let vehiculosCliente = [
  { idVehiculo: 1, placa: "ABC-123", marca: "Toyota", modelo: "Yaris", anio: 2019, kilometraje: 58200, idCliente: 3, nombreCliente: "Lucía Fernández", activo: true },
  { idVehiculo: 2, placa: "BKR-482", marca: "Hyundai", modelo: "Accent", anio: 2021, kilometraje: 31500, idCliente: 3, nombreCliente: "Lucía Fernández", activo: true },
];
const sinActivo = ({ activo, ...v }) => (void activo, { ...v });
const vehiculoDelCliente = (id) => {
  const v = vehiculosCliente.find((x) => x.idVehiculo === Number(id) && x.activo);
  if (!v) throw conEstado("Vehiculo no encontrado", 404);
  return v;
};

export async function listarVehiculosCliente() {
  await esperar();
  return vehiculosCliente.filter((v) => v.activo).map(sinActivo);
}

export async function registrarVehiculoCliente({ placa, marca, modelo, anio, kilometraje }) {
  await esperar(400);
  const p = String(placa ?? "").trim().toUpperCase();
  if (!/^[A-Z0-9]{3}-[0-9]{3}$/.test(p)) throw conEstado("Formato de placa inválido. Usa el formato ABC-123", 400);
  // Como en el backend, una placa eliminada sigue registrada en el sistema
  if (PLACAS_OTROS_CLIENTES.includes(p) || vehiculosCliente.some((v) => v.placa === p)) {
    throw conEstado("La placa ya se encuentra registrada en el sistema", 409);
  }
  const v = {
    idVehiculo: Math.max(0, ...vehiculosCliente.map((x) => x.idVehiculo)) + 1,
    placa: p, marca, modelo, anio, kilometraje: kilometraje ?? 0,
    idCliente: 3, nombreCliente: perfilCliente.nombre, activo: true,
  };
  vehiculosCliente = [...vehiculosCliente, v];
  return sinActivo(v);
}

export async function editarVehiculoCliente(id, { marca, modelo, anio, kilometraje }) {
  await esperar(400);
  const v = vehiculoDelCliente(id);
  if (!marca?.trim()) throw conEstado("La marca es obligatoria", 400);
  if (!modelo?.trim()) throw conEstado("El modelo es obligatorio", 400);
  if (anio == null) throw conEstado("El año es obligatorio", 400);
  const nuevo = kilometraje ?? v.kilometraje;
  if (nuevo < 0) throw conEstado("El kilometraje no puede ser negativo", 400);
  if (nuevo < v.kilometraje) throw conEstado(`El kilometraje no puede ser menor al registrado (${v.kilometraje} km)`, 400);
  Object.assign(v, { marca: marca.trim(), modelo: modelo.trim(), anio, kilometraje: nuevo });
  return sinActivo(v);
}

export async function eliminarVehiculoCliente(id) {
  await esperar(400);
  vehiculoDelCliente(id).activo = false;
}

/* ---------- Reserva de citas del cliente (CLI-03) ---------- */

// Mecánicos que atienden reservas: horario por día de la semana (1 = lunes ... 7 = domingo; el taller cierra domingo)
const MECANICOS_RESERVA = [
  { idMecanico: 2, nombre: "Carlos Quispe", aniosExperiencia: 8, especialidades: ["Frenos", "Mecánica general", "Suspensión"], calificacionPromedio: 4.7, totalResenas: 23, dias: [1, 2, 3, 4, 5, 6], tramos: [["08:00", "13:00"], ["14:00", "18:00"]] },
  { idMecanico: 6, nombre: "Miguel Rojas", aniosExperiencia: 5, especialidades: ["Electricidad automotriz", "Mecánica general", "Motor"], calificacionPromedio: 4.4, totalResenas: 11, dias: [1, 2, 3, 4, 5], tramos: [["09:00", "13:00"], ["14:00", "17:00"]] },
  { idMecanico: 10, nombre: "Luis Chávez", aniosExperiencia: 12, especialidades: ["Frenos", "Motor"], calificacionPromedio: null, totalResenas: 0, dias: [2, 4, 6], tramos: [["08:00", "14:00"]] },
];
const ESPECIALIDAD_SERVICIO = (nombre) => (/fren/i.test(nombre) ? "Frenos" : /motor|afinamiento/i.test(nombre) ? "Motor" : null);
const momentoLima = () => {
  const t = ahoraLima();
  return { fecha: t.slice(0, 10), hora: t.slice(11, 16) };
};
const aMinutos = (h) => Number(h.slice(0, 2)) * 60 + Number(h.slice(3, 5));
const aHora = (m) => `${dosCifras(Math.floor(m / 60))}:${dosCifras(m % 60)}`;
// Citas del cliente de la demo (las nuevas reservas se agregan aquí). Las PENDIENTE y CONFIRMADA ocupan su turno.
let citasReservadas = [
  [131, 3, "09:00", 60, "CONFIRMADA", 2, 2, "ABC-123", "AS-2026-00131"],
  [133, 1, "10:00", 45, "PENDIENTE", 1, 6, "BKR-482", "AS-2026-00133"],
  [128, 0, "08:00", 60, "ESPERA_APROBACION", 3, 2, "BKR-482", "AS-2026-00128"],
  [125, -1, "15:00", 40, "FINALIZADA", 4, 6, "ABC-123", "AS-2026-00125"],
  [124, -48, "08:30", 45, "PAGADO", 1, 2, "ABC-123", "AS-2026-00080"],
  [110, -20, "11:00", 60, "CANCELADA", 2, 6, "ABC-123", "AS-2026-00110"],
  [105, -12, "16:00", 60, "RECHAZADA", 3, 2, "BKR-482", "AS-2026-00105"],
].map(([idCita, dias, horaInicio, duracion, estado, idServicio, idMecanico, placa, ticket]) => {
  const s = servicios.find((x) => x.id === idServicio);
  const fin = Number(horaInicio.slice(0, 2)) * 60 + Number(horaInicio.slice(3, 5)) + duracion;
  return {
    idCita, ticket, estado, fecha: fechaRelativa(dias), horaInicio, horaFin: `${dosCifras(Math.floor(fin / 60))}:${dosCifras(fin % 60)}`,
    precio: s.precioBase, servicio: s.nombre, mecanico: ["", "", "Carlos Quispe", "", "", "", "Miguel Rojas"][idMecanico], placa, idMecanico, idServicio,
  };
});
const OCUPAN_TURNO = ["PENDIENTE", "CONFIRMADA"];
let bloqueoReserva = null;
let siguienteCitaReserva = 140;

function servicioReserva(id) {
  const s = servicios.find((x) => x.id === Number(id) && x.activo);
  if (!s) throw conEstado("Servicio no encontrado", 404);
  return s;
}

export async function catalogoReserva() {
  await esperar();
  return servicios
    .filter((s) => s.activo)
    .map((s) => ({ idServicio: s.id, nombre: s.nombre, descripcion: s.descripcion, precioBase: s.precioBase, duracionMinutos: s.duracionMinutos, categoria: s.categoria, especialidad: ESPECIALIDAD_SERVICIO(s.nombre) }))
    .sort((a, b) => a.nombre.localeCompare(b.nombre));
}

export async function mecanicosReserva(idServicio) {
  await esperar();
  const requerida = ESPECIALIDAD_SERVICIO(servicioReserva(idServicio).nombre);
  return MECANICOS_RESERVA.filter((m) => !requerida || m.especialidades.includes(requerida)).map(({ dias, tramos, ...m }) => (void dias, void tramos, { ...m }));
}

// Mismo cálculo que TurnoService: tramos del mecánico cortados por la duración del servicio, sin los ocupados
function turnosLibresReserva(idServicio, idMecanico, fecha, conBloqueoPropio) {
  const s = servicioReserva(idServicio);
  const m = MECANICOS_RESERVA.find((x) => x.idMecanico === Number(idMecanico));
  if (!m) throw conEstado("Mecanico no encontrado", 404);
  const ahora = momentoLima();
  if (fecha < ahora.fecha) throw conEstado("La fecha no puede ser pasada", 400);
  const dia = new Date(`${fecha}T12:00:00Z`).getUTCDay() || 7;
  if (!m.dias.includes(dia)) return [];
  const libres = [];
  m.tramos.forEach(([ini, fin]) => {
    for (let t = aMinutos(ini); t + s.duracionMinutos <= aMinutos(fin); t += s.duracionMinutos) {
      const inicio = aHora(t);
      const final = aHora(t + s.duracionMinutos);
      const cruza = (a, b) => inicio < b && final > a;
      // Agenda de ejemplo: algunos turnos ya ocupados por otros clientes
      const ocupadoDemo = (Number(fecha.slice(8, 10)) + m.idMecanico + t / 15) % 5 === 0;
      const ocupado = citasReservadas.some((c) => OCUPAN_TURNO.includes(c.estado) && c.idMecanico === m.idMecanico && c.fecha === fecha && cruza(c.horaInicio, c.horaFin));
      const bloqueado = !conBloqueoPropio && bloqueoReserva && bloqueoReserva.idMecanico === m.idMecanico && bloqueoReserva.fecha === fecha && cruza(bloqueoReserva.horaInicio, bloqueoReserva.horaFin);
      const yaPaso = fecha === ahora.fecha && inicio <= ahora.hora;
      if (!ocupadoDemo && !ocupado && !bloqueado && !yaPaso) libres.push({ horaInicio: inicio, horaFin: final });
    }
  });
  return libres;
}

export async function turnosReserva(idServicio, idMecanico, fecha) {
  await esperar();
  return turnosLibresReserva(idServicio, idMecanico, fecha, false);
}

export async function bloquearTurnoReserva({ idServicio, idMecanico, fecha, horaInicio }) {
  await esperar(250);
  const s = servicioReserva(idServicio);
  if (!turnosLibresReserva(idServicio, idMecanico, fecha, true).some((t) => t.horaInicio === horaInicio)) throw conEstado("El turno no esta disponible", 409);
  bloqueoReserva = { idBloqueo: Date.now(), idMecanico: Number(idMecanico), fecha, horaInicio, horaFin: aHora(aMinutos(horaInicio) + s.duracionMinutos) };
  return { ...bloqueoReserva, expiraEn: null, segundosRestantes: 600 };
}

export async function liberarTurnoReserva(id) {
  if (bloqueoReserva?.idBloqueo === id) bloqueoReserva = null;
}

export async function reservarCitaCliente({ idServicio, idMecanico, idVehiculo, fecha, horaInicio }) {
  await esperar(500);
  const s = servicioReserva(idServicio);
  const m = MECANICOS_RESERVA.find((x) => x.idMecanico === Number(idMecanico));
  const v = vehiculoDelCliente(idVehiculo);
  if (!turnosLibresReserva(idServicio, idMecanico, fecha, true).some((t) => t.horaInicio === horaInicio)) throw conEstado("El turno ya no esta disponible", 409);
  const c = {
    idCita: siguienteCitaReserva++, estado: "PENDIENTE", fecha, horaInicio, horaFin: aHora(aMinutos(horaInicio) + s.duracionMinutos),
    precio: s.precioBase, servicio: s.nombre, mecanico: m.nombre, placa: v.placa, idMecanico: m.idMecanico, idServicio: s.id,
  };
  c.ticket = `AS-${fecha.slice(0, 4)}-${String(c.idCita).padStart(5, "0")}`;
  citasReservadas = [...citasReservadas, c];
  bloqueoReserva = null;
  return citaParaCliente(c);
}

/* ---------- Mis citas del cliente (CLI-04) ---------- */

const citaParaCliente = ({ idMecanico, idServicio, ...c }) => (void idMecanico, void idServicio, { ...c });
const inicioCita = (c) => new Date(`${c.fecha}T${c.horaInicio}:00-05:00`).getTime();
function citaDelCliente(id) {
  const c = citasReservadas.find((x) => x.idCita === Number(id));
  if (!c) throw conEstado("Cita no encontrada", 404);
  return c;
}

export async function listarMisCitas(futuras) {
  await esperar();
  const hoy = momentoLima().fecha;
  return citasReservadas
    .filter((c) => !futuras || (c.fecha >= hoy && OCUPAN_TURNO.includes(c.estado)))
    .sort((a, b) => `${a.fecha}${a.horaInicio}`.localeCompare(`${b.fecha}${b.horaInicio}`))
    .map(citaParaCliente);
}

export async function cancelarMiCita(id) {
  await esperar(400);
  const c = citaDelCliente(id);
  if (!OCUPAN_TURNO.includes(c.estado)) throw conEstado(`La cita ya no esta activa (estado: ${c.estado})`, 409);
  if (inicioCita(c) - Date.now() < 2 * 3600 * 1000) throw conEstado("Solo se puede cancelar con al menos 2 horas de anticipacion", 400);
  c.estado = "CANCELADA";
  return citaParaCliente(c);
}

export async function reprogramarMiCita(id, { fecha, horaInicio }) {
  await esperar(400);
  const c = citaDelCliente(id);
  if (!OCUPAN_TURNO.includes(c.estado)) throw conEstado(`La cita ya no esta activa (estado: ${c.estado})`, 409);
  if (inicioCita(c) - Date.now() < 24 * 3600 * 1000) throw conEstado("Solo se puede reprogramar con al menos 24 horas de anticipacion", 400);
  if (c.fecha === fecha && c.horaInicio === horaInicio) throw conEstado("La cita ya esta en ese turno", 409);
  if (!turnosLibresReserva(c.idServicio, c.idMecanico, fecha, true).some((t) => t.horaInicio === horaInicio)) throw conEstado("El nuevo turno no esta disponible", 409);
  const s = servicioReserva(c.idServicio);
  Object.assign(c, { fecha, horaInicio, horaFin: aHora(aMinutos(horaInicio) + s.duracionMinutos), estado: "PENDIENTE" });
  return citaParaCliente(c);
}

/* ---------- Seguimiento y presupuesto del cliente (CLI-05) ---------- */

// Orden de trabajo de las citas de la demo que ya se atendieron o se están atendiendo
const ordenesCliente = {
  128: { idOrden: 98, inicio: enHora(0, "08:05"), diagnostico: enHora(0, "08:40"), fin: null, avance: 40 },
  125: { idOrden: 95, inicio: enHora(-1, "15:02"), diagnostico: enHora(-1, "15:15"), fin: enHora(-1, "15:52"), avance: 100 },
  124: { idOrden: 80, inicio: enHora(-48, "08:31"), diagnostico: enHora(-48, "08:45"), fin: enHora(-48, "09:20"), avance: 100 },
};
const presupuestosCliente = {
  98: {
    idPresupuesto: 31, idOrden: 98, idCita: 128, estado: "PENDIENTE", fechaCreacion: enHora(0, "09:10"), fechaDecision: null,
    descripcion: "Al revisar los frenos encontramos las pastillas delanteras casi gastadas y los discos con rayas. Recomendamos cambiarlas hoy para que frene con seguridad.",
    items: [
      { tipo: "REPUESTO", descripcion: "Pastillas de freno delanteras (juego)", cantidad: 1, precioUnitario: 145, subtotal: 145 },
      { tipo: "MANO_OBRA", descripcion: "Rectificado de discos delanteros", cantidad: 2, precioUnitario: 30, subtotal: 60 },
      { tipo: "REPUESTO", descripcion: "Líquido de frenos DOT 4 (500 ml)", cantidad: 1, precioUnitario: 35, subtotal: 35 },
    ],
    total: 240,
    deficiencias: [
      { idDeficiencia: 61, idOrden: 98, categoria: "FRENOS", severidad: "CRITICA", descripcion: "Pastillas delanteras con 10 % de vida útil; rozan el disco al frenar.", fechaRegistro: enHora(0, "08:35"), fotos: [{ idFoto: 91, src: fotoEjemplo("Pastillas delanteras", ["#7c2d12", "#ea580c"]) }, { idFoto: 92, src: fotoEjemplo("Disco con rayas", ["#1e293b", "#64748b"]) }] },
      { idDeficiencia: 62, idOrden: 98, categoria: "FLUIDOS", severidad: "MODERADA", descripcion: "Líquido de frenos oscuro; le toca cambio.", fechaRegistro: enHora(0, "08:38"), fotos: [] },
    ],
  },
  95: {
    idPresupuesto: 29, idOrden: 95, idCita: 125, estado: "APROBADO", fechaCreacion: enHora(-1, "15:20"), fechaDecision: enHora(-1, "15:24"),
    descripcion: "El escáner marcó una falla en el sensor de oxígeno. Se reemplazó para corregir el consumo de combustible.",
    items: [
      { tipo: "REPUESTO", descripcion: "Sensor de oxígeno", cantidad: 1, precioUnitario: 180, subtotal: 180 },
      { tipo: "MANO_OBRA", descripcion: "Cambio del sensor", cantidad: 1, precioUnitario: 40, subtotal: 40 },
    ],
    total: 220,
    deficiencias: [{ idDeficiencia: 55, idOrden: 95, categoria: "MOTOR", severidad: "MODERADA", descripcion: "Código P0134: sensor de oxígeno sin respuesta.", fechaRegistro: enHora(-1, "15:14"), fotos: [] }],
  },
};


export async function seguimientoCita(idCita) {
  await esperar();
  const c = citaDelCliente(idCita);
  const o = ordenesCliente[c.idCita] ?? null;
  const p = o ? presupuestosCliente[o.idOrden] : null;
  const linea = [{ clave: "RESERVADA", titulo: "Cita reservada", completado: true, fecha: null, detalle: `Turno del ${c.fecha} a las ${c.horaInicio}` }];
  if (c.estado === "CANCELADA" || c.estado === "RECHAZADA") {
    linea.push({ clave: c.estado, titulo: c.estado === "RECHAZADA" ? "Cita rechazada" : "Cita cancelada", completado: true, fecha: null, detalle: c.estado === "RECHAZADA" ? "El mecánico tiene una capacitación ese día." : null });
  } else {
    const terminada = c.estado === "FINALIZADA" || c.estado === "PAGADO";
    linea.push({ clave: "CONFIRMADA", titulo: "Cita confirmada por el mecánico", completado: c.estado !== "PENDIENTE", fecha: null, detalle: null });
    linea.push({ clave: "RECIBIDO", titulo: "Vehículo recibido en el taller", completado: !!o, fecha: o?.inicio ?? null, detalle: null });
    linea.push({ clave: "DIAGNOSTICO", titulo: "Diagnóstico registrado", completado: !!o?.diagnostico, fecha: o?.diagnostico ?? null, detalle: null });
    if (p) {
      const decidido = !!p.fechaDecision;
      linea.push({ clave: "PRESUPUESTO", titulo: decidido ? "Presupuesto respondido" : "Presupuesto enviado (espera tu aprobación)", completado: decidido, fecha: decidido ? p.fechaDecision : p.fechaCreacion, detalle: `Total S/ ${p.total.toFixed(2)} - ${p.estado}` });
    }
    linea.push({ clave: "EN_REPARACION", titulo: "Reparación en curso", completado: terminada, fecha: null, detalle: o ? `Avance: ${o.avance} %` : null });
    linea.push({ clave: "FINALIZADA", titulo: "Vehículo listo para recoger", completado: terminada, fecha: o?.fin ?? null, detalle: null });
    linea.push({ clave: "PAGADO", titulo: "Servicio pagado", completado: c.estado === "PAGADO", fecha: null, detalle: null });
  }

  return { idCita: c.idCita, ticket: c.ticket, estado: c.estado, servicio: c.servicio, placa: c.placa, mecanico: c.mecanico, fecha: c.fecha, horaInicio: c.horaInicio, porcentajeAvance: o?.avance ?? 0, lineaDeTiempo: linea };
}

export async function presupuestoCita(idCita) {
  await esperar();
  const c = citaDelCliente(idCita);
  const o = ordenesCliente[c.idCita];
  if (!o) throw conEstado("La cita aun no tiene presupuesto", 404);
  const p = presupuestosCliente[o.idOrden];
  if (!p) throw conEstado("La orden aun no tiene presupuesto", 404);
  return copia(p);
}

export async function decidirPresupuesto(idPresupuesto, { decision }) {
  await esperar(450);
  const p = Object.values(presupuestosCliente).find((x) => x.idPresupuesto === Number(idPresupuesto));
  if (!p) throw conEstado("Presupuesto no encontrado", 404);
  if (p.estado !== "PENDIENTE") throw conEstado(`El presupuesto ya fue ${p.estado}`, 409);
  Object.assign(p, { estado: decision, fechaDecision: ahoraLima() });
  citaDelCliente(p.idCita).estado = "EN_PROCESO";
  return copia(p);
}
