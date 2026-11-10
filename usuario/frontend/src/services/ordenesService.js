import { apiFetch, apiArchivo } from "./api";
import * as prueba from "./datosPrueba";

/* Orden de trabajo del mecánico (TEC3.2). La inspección (checklist, deficiencias y fotos)
   se registra desde la app móvil; la web la muestra.
   Backend de Farid (rama main):
     GET /mechanics/me/work-orders         -> órdenes activas del mecánico, EN_PROCESO o ESPERA_APROBACION (TEC3.1)
       [{ idOrden, idCita, ticket, estado, fechaInicio, servicio, placa }]
     GET /work-orders/{idOrden}/checklist  -> checklist de recepción; 404 si aún no se registra (T42.1)
       { idChecklist, idOrden, fechaRegistro, observacionGeneral, items: [{ elemento, estado OK|OBSERVADO, observacion }] }
     GET /work-orders/{idOrden}/deficiencies -> deficiencias con sus fotos (T43.1)
       [{ idDeficiencia, categoria, severidad LEVE|MODERADA|CRITICA, descripcion, fechaRegistro, fotos: [{ idFoto, url }] }]
     GET /photos/{idFoto}                  -> la imagen JPG o PNG (T44.1); url = "/photos/{idFoto}".
       Pide el token JWT (solo el mecánico asignado y el cliente dueño), por eso se descarga con apiArchivo.
     Diagnóstico técnico (T45.1):
       GET /work-orders/{idOrden}/diagnosis -> { idOrden, diagnostico, fechaDiagnostico }; 404 si aún no se registra
       PUT /work-orders/{idOrden}/diagnosis { diagnostico } (obligatorio, máx. 2000) -> lo crea o lo reemplaza
     Tareas de la orden (T49.1): las tres rutas devuelven el resumen completo
       GET  /work-orders/{idOrden}/tasks
       POST /work-orders/{idOrden}/tasks            { descripcion } (obligatoria, máx. 200)
       PUT  /work-orders/{idOrden}/tasks/{idTarea}  { completada: true|false }
       -> { idOrden, totalTareas, completadas, porcentajeAvance, tareas: [{ idTarea, descripcion, completada,
            fechaCreacion, fechaCompletada }] }
     Presupuesto (T46.1): requiere el diagnóstico registrado (409 si falta)
       GET  /work-orders/{idOrden}/quote -> el último presupuesto de la orden; 404 si aún no hay
       POST /work-orders/{idOrden}/quote { descripcion (máx. 500), items: [{ tipo MANO_OBRA|REPUESTO,
            descripcion (máx. 200), cantidad (entero ≥ 1), precioUnitario (≥ 0) }] (1 a 30) }
         -> { idPresupuesto, idOrden, idCita, estado PENDIENTE|APROBADO|RECHAZADO, descripcion, fechaCreacion,
              fechaDecision, items: [{ tipo, descripcion, cantidad, precioUnitario, subtotal }], total, deficiencias }
         Al enviarlo, la cita pasa a ESPERA_APROBACION hasta que el cliente decide en la app;
         con la decisión (aprobado o rechazado) vuelve a EN_PROCESO.
     GET /materials?buscar= -> materiales del almacén (MECANICO y ADMIN), para elegir repuestos
       [{ idMaterial, codigo, nombre, unidad, costoUnitario, stockActual, stockMinimo, bajoStock }]
     Solicitudes de repuestos al almacén (T50.1):
       GET  /work-orders/{idOrden}/material-requests
       POST /work-orders/{idOrden}/material-requests { idMaterial, cantidad (≥ 1), motivo (opcional, máx. 200) }
         409 si no hay stock suficiente ("Stock insuficiente de X: disponible 2, solicitado 3")
       -> { idSolicitud, idMaterial, codigoMaterial, nombreMaterial, cantidad, stockDisponible, motivo,
            estado PENDIENTE|APROBADA|RECHAZADA, solicitadoPor, fechaSolicitud, atendidoPor, fechaAtencion, observacion }
       El administrador la atiende en su bandeja (T59); al aprobarla se descuenta el stock y se suma a la orden.
   Todo lo que cambia la orden responde 409 si no está EN_PROCESO
   ("La orden no esta EN_PROCESO (estado: ESPERA_APROBACION)").
   Con VITE_DATOS_PRUEBA=true todo sale de datosPrueba.js. */

export const ELEMENTOS_CHECKLIST = [
  { clave: "LUCES", texto: "Luces" },
  { clave: "FRENOS", texto: "Frenos" },
  { clave: "NEUMATICOS", texto: "Neumáticos" },
  { clave: "FLUIDOS", texto: "Fluidos" },
  { clave: "CARROCERIA", texto: "Carrocería" },
];

export const ESTADOS_ITEM = { OK: "OK", OBSERVADO: "Observado", SIN_REVISAR: "Sin revisar" };

// De la más grave a la más leve
export const SEVERIDADES = {
  CRITICA: { texto: "Crítica", plural: "críticas", orden: 0 },
  MODERADA: { texto: "Moderada", plural: "moderadas", orden: 1 },
  LEVE: { texto: "Leve", plural: "leves", orden: 2 },
};

const CATEGORIAS = {
  MOTOR: "Motor",
  FRENOS: "Frenos",
  SUSPENSION: "Suspensión",
  DIRECCION: "Dirección",
  TRANSMISION: "Transmisión",
  ELECTRICO: "Sistema eléctrico",
  NEUMATICOS: "Neumáticos",
  FLUIDOS: "Fluidos",
  LUCES: "Luces",
  CARROCERIA: "Carrocería",
  OTRO: "Otro",
};

// "LUCES" / "Luces" / "luces" -> "LUCES"; "CRÍTICA" -> "CRITICA"
const codigo = (texto) =>
  String(texto ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .trim()
    .toUpperCase()
    .replace(/\s+/g, "_");

export function textoCategoria(categoria) {
  const c = codigo(categoria);
  if (CATEGORIAS[c]) return CATEGORIAS[c];
  const libre = String(categoria ?? "").replace(/_/g, " ").toLowerCase();
  return libre ? libre.charAt(0).toUpperCase() + libre.slice(1) : "Sin categoría";
}

const esNoEncontrado = (e) => e.status === 404 || /no tiene|no encontrad/i.test(e.message);

/* ---------- Normalizadores ---------- */

function normalizarOrden(o = {}) {
  return {
    idOrden: o.idOrden ?? o.id,
    idCita: o.idCita,
    ticket: o.ticket ?? "",
    estado: codigo(o.estado || "EN_PROCESO"),
    fechaInicio: o.fechaInicio ?? "",
    fechaFin: o.fechaFin ?? "",
    servicio: o.servicio ?? "",
    placa: o.placa ?? "",
  };
}

function normalizarChecklist(c) {
  if (!c) return null;
  const items = (c.items ?? []).map((i) => ({
    elemento: codigo(i.elemento),
    estado: codigo(i.estado) === "OK" ? "OK" : "OBSERVADO",
    observacion: i.observacion ?? "",
  }));
  if (!items.length) return null;
  return {
    id: c.idChecklist ?? c.id,
    fechaRegistro: c.fechaRegistro ?? "",
    observacionGeneral: c.observacionGeneral ?? "",
    items,
  };
}

function normalizarFoto(f) {
  const idFoto = f.idFoto ?? f.id ?? null;
  const url = f.url ?? (idFoto !== null ? `/photos/${idFoto}` : "");
  return { clave: String(idFoto ?? url ?? f.src), idFoto, url, src: f.src ?? "" };
}

function normalizarDeficiencia(d = {}) {
  const severidad = codigo(d.severidad);
  return {
    id: d.idDeficiencia ?? d.id,
    categoria: d.categoria ?? "",
    severidad: SEVERIDADES[severidad] ? severidad : "LEVE",
    descripcion: d.descripcion ?? "",
    fechaRegistro: d.fechaRegistro ?? "",
    fotos: (d.fotos ?? []).map(normalizarFoto),
  };
}

/* ---------- Llamadas ---------- */

// Orden de la cita, o null si la atención aún no se inició (o no se encuentra)
export async function obtenerOrdenDeCita(idCita) {
  try {
    if (prueba.DATOS_PRUEBA) return normalizarOrden(await prueba.obtenerOrdenDeCita(idCita));

    // 1) Entre las órdenes activas del mecánico
    const activas = await apiFetch("/mechanics/me/work-orders");
    const activa = (Array.isArray(activas) ? activas : []).find((o) => String(o.idCita) === String(idCita));
    if (activa) return normalizarOrden(activa);

    // 2) Orden de una cita ya cerrada (cuando el backend tenga esta ruta)
    return normalizarOrden(await apiFetch(`/appointments/${idCita}/work-order`));
  } catch (e) {
    if (esNoEncontrado(e)) return null;
    throw e;
  }
}

// Checklist de recepción, o null si todavía no se registra desde la app
export async function obtenerChecklist(idOrden) {
  try {
    const datos = prueba.DATOS_PRUEBA
      ? await prueba.obtenerChecklist(idOrden)
      : await apiFetch(`/work-orders/${idOrden}/checklist`);
    return normalizarChecklist(datos);
  } catch (e) {
    if (esNoEncontrado(e)) return null;
    throw e;
  }
}

/* ---------- Diagnóstico técnico (T45.2) ---------- */

export const MIN_DIAGNOSTICO = 10;
export const MAX_DIAGNOSTICO = 2000;

function normalizarDiagnostico(d) {
  const texto = String(d?.diagnostico ?? d?.descripcion ?? d?.texto ?? "").trim();
  return texto ? { texto, fechaRegistro: d.fechaDiagnostico ?? d.fechaRegistro ?? "" } : null;
}

// Diagnóstico de la orden, o null si aún no se registra (o el backend todavía no tiene la ruta)
export async function obtenerDiagnostico(idOrden) {
  try {
    const datos = prueba.DATOS_PRUEBA
      ? await prueba.obtenerDiagnostico(idOrden)
      : await apiFetch(`/work-orders/${idOrden}/diagnosis`);
    return normalizarDiagnostico(datos);
  } catch (e) {
    if (esNoEncontrado(e)) return null;
    throw e;
  }
}

export async function guardarDiagnostico(idOrden, texto) {
  const cuerpo = { diagnostico: texto.trim() };
  try {
    const datos = prueba.DATOS_PRUEBA
      ? await prueba.guardarDiagnostico(idOrden, cuerpo.diagnostico)
      : await apiFetch(`/work-orders/${idOrden}/diagnosis`, { metodo: "PUT", cuerpo });
    return normalizarDiagnostico({ ...cuerpo, ...datos }) ?? { texto: cuerpo.diagnostico, fechaRegistro: "" };
  } catch (e) {
    throw mensajeConflicto(e);
  }
}

// "La orden no esta EN_PROCESO (estado: ESPERA_APROBACION)" -> texto claro para el mecánico
export function mensajeConflicto(e) {
  if (e.status !== 409) return e;
  const estado = /estado: ([A-Z_]+)/.exec(e.message)?.[1];
  if (!estado) return e;
  const textos = {
    ESPERA_APROBACION: "La orden está esperando que el cliente apruebe el presupuesto; por ahora no se puede modificar.",
    FINALIZADA: "La orden ya fue finalizada; no se puede modificar.",
  };
  return Object.assign(new Error(textos[estado] ?? `La orden ya no está en proceso (${estado}).`), {
    status: 409,
    estado,
    cause: e,
  });
}

/* ---------- Tareas con avance (T49.2) ---------- */

export const MAX_TAREA = 200;

function normalizarTareas(r = {}) {
  const tareas = (r.tareas ?? []).map((t) => ({
    id: t.idTarea ?? t.id,
    descripcion: t.descripcion ?? "",
    completada: !!t.completada,
    fechaCreacion: t.fechaCreacion ?? "",
    fechaCompletada: t.fechaCompletada ?? "",
  }));
  const total = r.totalTareas ?? tareas.length;
  const completadas = r.completadas ?? tareas.filter((t) => t.completada).length;
  return {
    tareas,
    total,
    completadas,
    porcentaje: r.porcentajeAvance ?? (total ? Math.round((completadas * 100) / total) : 0),
  };
}

export const TAREAS_VACIAS = normalizarTareas();

export async function listarTareas(idOrden) {
  try {
    const datos = prueba.DATOS_PRUEBA ? await prueba.listarTareas(idOrden) : await apiFetch(`/work-orders/${idOrden}/tasks`);
    return normalizarTareas(datos);
  } catch (e) {
    if (esNoEncontrado(e)) return TAREAS_VACIAS;
    throw e;
  }
}

export async function agregarTarea(idOrden, descripcion) {
  const cuerpo = { descripcion: descripcion.trim() };
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.agregarTarea(idOrden, cuerpo.descripcion)
    : await apiFetch(`/work-orders/${idOrden}/tasks`, { metodo: "POST", cuerpo });
  return normalizarTareas(datos);
}

export async function marcarTarea(idOrden, idTarea, completada) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.marcarTarea(idOrden, idTarea, completada)
    : await apiFetch(`/work-orders/${idOrden}/tasks/${idTarea}`, { metodo: "PUT", cuerpo: { completada } });
  return normalizarTareas(datos);
}

/* ---------- Presupuesto (T46.2) ---------- */

export const MAX_DESC_PRESUPUESTO = 500;
export const MAX_DESC_ITEM = 200;
export const MAX_ITEMS = 30;
export const TIPOS_ITEM = { MANO_OBRA: "Mano de obra", REPUESTO: "Repuesto" };
export const ESTADOS_PRESUPUESTO = {
  PENDIENTE: "Esperando al cliente",
  APROBADO: "Aprobado",
  RECHAZADO: "Rechazado",
};

const dinero = (n) => Math.round(Number(n ?? 0) * 100) / 100;

function normalizarPresupuesto(p) {
  if (!p) return null;
  const items = (p.items ?? []).map((i) => ({
    tipo: codigo(i.tipo) === "REPUESTO" ? "REPUESTO" : "MANO_OBRA",
    descripcion: i.descripcion ?? "",
    cantidad: Number(i.cantidad ?? 1),
    precioUnitario: dinero(i.precioUnitario),
    subtotal: dinero(i.subtotal ?? Number(i.precioUnitario ?? 0) * Number(i.cantidad ?? 1)),
  }));
  const estado = codigo(p.estado);
  return {
    id: p.idPresupuesto ?? p.id,
    estado: ESTADOS_PRESUPUESTO[estado] ? estado : "PENDIENTE",
    descripcion: p.descripcion ?? "",
    fechaCreacion: p.fechaCreacion ?? "",
    fechaDecision: p.fechaDecision ?? "",
    items,
    total: dinero(p.total ?? items.reduce((s, i) => s + i.subtotal, 0)),
  };
}

// Último presupuesto de la orden, o null si todavía no se arma
export async function obtenerPresupuesto(idOrden) {
  try {
    const datos = prueba.DATOS_PRUEBA ? await prueba.obtenerPresupuesto(idOrden) : await apiFetch(`/work-orders/${idOrden}/quote`);
    return normalizarPresupuesto(datos);
  } catch (e) {
    if (esNoEncontrado(e)) return null;
    throw e;
  }
}

export async function enviarPresupuesto(idOrden, { descripcion, items }) {
  const cuerpo = {
    descripcion: descripcion.trim(),
    items: items.map((i) => ({
      tipo: i.tipo,
      descripcion: i.descripcion.trim(),
      cantidad: Number(i.cantidad),
      precioUnitario: dinero(i.precioUnitario),
    })),
  };
  try {
    const datos = prueba.DATOS_PRUEBA
      ? await prueba.enviarPresupuesto(idOrden, cuerpo)
      : await apiFetch(`/work-orders/${idOrden}/quote`, { metodo: "POST", cuerpo });
    return normalizarPresupuesto(datos);
  } catch (e) {
    throw mensajeConflicto(e);
  }
}

// Materiales del almacén para elegir repuestos (se piden una vez por pantalla)
export async function listarMaterialesTaller() {
  const datos = prueba.DATOS_PRUEBA ? await prueba.listarMaterialesTaller() : await apiFetch("/materials");
  return (Array.isArray(datos) ? datos : []).map((m) => ({
    id: m.idMaterial ?? m.id,
    codigo: String(m.codigo ?? "").toUpperCase(),
    nombre: m.nombre ?? "",
    unidad: String(m.unidad ?? "unidad").toLowerCase(),
    costoUnitario: dinero(m.costoUnitario),
    stockActual: Number(m.stockActual ?? 0),
    stockMinimo: Number(m.stockMinimo ?? 0),
    bajoStock: m.bajoStock ?? Number(m.stockActual ?? 0) <= Number(m.stockMinimo ?? 0),
  }));
}

/* ---------- Solicitudes de repuestos (T50.2) ---------- */

export const MAX_MOTIVO = 200;
export const ESTADOS_SOLICITUD = { PENDIENTE: "Pendiente", APROBADA: "Aprobada", RECHAZADA: "Rechazada" };

function normalizarSolicitud(s = {}) {
  const estado = codigo(s.estado);
  return {
    id: s.idSolicitud ?? s.id,
    idMaterial: s.idMaterial,
    codigo: String(s.codigoMaterial ?? "").toUpperCase(),
    nombre: s.nombreMaterial ?? "",
    cantidad: Number(s.cantidad ?? 0),
    stockDisponible: s.stockDisponible ?? null,
    motivo: s.motivo ?? "",
    estado: ESTADOS_SOLICITUD[estado] ? estado : "PENDIENTE",
    fechaSolicitud: s.fechaSolicitud ?? "",
    atendidoPor: s.atendidoPor ?? "",
    fechaAtencion: s.fechaAtencion ?? "",
    observacion: s.observacion ?? "",
  };
}

export async function listarSolicitudes(idOrden) {
  try {
    const datos = prueba.DATOS_PRUEBA
      ? await prueba.listarSolicitudes(idOrden)
      : await apiFetch(`/work-orders/${idOrden}/material-requests`);
    return (Array.isArray(datos) ? datos : []).map(normalizarSolicitud);
  } catch (e) {
    if (esNoEncontrado(e)) return [];
    throw e;
  }
}

export async function solicitarRepuesto(idOrden, { idMaterial, cantidad, motivo }) {
  const cuerpo = { idMaterial, cantidad: Number(cantidad), motivo: motivo.trim() || null };
  try {
    const datos = prueba.DATOS_PRUEBA
      ? await prueba.solicitarRepuesto(idOrden, cuerpo)
      : await apiFetch(`/work-orders/${idOrden}/material-requests`, { metodo: "POST", cuerpo });
    return normalizarSolicitud(datos);
  } catch (e) {
    throw mensajeConflicto(e);
  }
}

// Texto corto para el resumen: "1 pendiente · 2 aprobadas"
export function resumenSolicitudes(solicitudes) {
  const n = (e) => solicitudes.filter((s) => s.estado === e).length;
  const partes = [
    n("PENDIENTE") && `${n("PENDIENTE")} ${n("PENDIENTE") === 1 ? "pendiente" : "pendientes"}`,
    n("APROBADA") && `${n("APROBADA")} ${n("APROBADA") === 1 ? "aprobada" : "aprobadas"}`,
    n("RECHAZADA") && `${n("RECHAZADA")} ${n("RECHAZADA") === 1 ? "rechazada" : "rechazadas"}`,
  ].filter(Boolean);
  return partes.join(" · ");
}

const SOLES = new Intl.NumberFormat("es-PE", { style: "currency", currency: "PEN" });
export const formatoSoles = (n) => SOLES.format(Number(n ?? 0));

// Deficiencias de la orden, de la más grave a la más leve (y por hora de registro)
// Si el backend aún no tiene la ruta (404), se muestra la lista vacía.
export async function listarDeficiencias(idOrden) {
  let datos = [];
  try {
    datos = prueba.DATOS_PRUEBA
      ? await prueba.listarDeficiencias(idOrden)
      : await apiFetch(`/work-orders/${idOrden}/deficiencies`);
  } catch (e) {
    if (!esNoEncontrado(e)) throw e;
  }
  return (Array.isArray(datos) ? datos : [])
    .map(normalizarDeficiencia)
    .sort(
      (a, b) =>
        SEVERIDADES[a.severidad].orden - SEVERIDADES[b.severidad].orden ||
        a.fechaRegistro.localeCompare(b.fechaRegistro)
    );
}

// Dirección local de una foto. Se guarda en memoria para no descargarla dos veces
// (miniatura y visor). Las URL públicas o de ejemplo se usan tal cual.
const fotosCargadas = new Map();
export function cargarFoto(foto) {
  if (foto.src) return Promise.resolve(foto.src);
  if (/^(https?:|data:|blob:)/.test(foto.url)) return Promise.resolve(foto.url);
  if (!fotosCargadas.has(foto.clave)) {
    const promesa = apiArchivo(foto.url).then((blob) => {
      // Si no llega una imagen (p. ej., una página HTML por una ruta mal enrutada), se muestra el ícono de falla
      if (blob.type && !blob.type.startsWith("image/")) throw new Error("La respuesta no es una imagen.");
      return URL.createObjectURL(blob);
    });
    promesa.catch(() => fotosCargadas.delete(foto.clave));
    fotosCargadas.set(foto.clave, promesa);
  }
  return fotosCargadas.get(foto.clave);
}

/* ---------- Fechas y tiempos ("AAAA-MM-DDTHH:mm:ss", hora de Lima) ---------- */

export const fechaDe = (fechaHora) => String(fechaHora ?? "").slice(0, 10);

// Milisegundos de una fecha-hora de Lima (Perú no tiene horario de verano: UTC-5)
function marcaDe(fechaHora) {
  const texto = String(fechaHora ?? "");
  if (!texto) return NaN;
  return Date.parse(/([zZ]|[+-]\d\d:?\d\d)$/.test(texto) ? texto : `${texto.slice(0, 19)}-05:00`);
}

// Minutos completos entre dos fechas-hora (la segunda puede ser una marca de Date.now())
export function minutosEntre(desde, hasta) {
  const fin = typeof hasta === "number" ? hasta : marcaDe(hasta);
  const minutos = Math.floor((fin - marcaDe(desde)) / 60000);
  return Number.isNaN(minutos) ? null : minutos;
}
