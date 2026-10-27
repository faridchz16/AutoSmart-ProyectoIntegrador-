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
