import { apiFetch, apiArchivo } from "./api";
import * as prueba from "./datosPrueba";

/* Orden de trabajo del mecánico (TEC3.2). La inspección (checklist, deficiencias y fotos)
   se registra desde la app móvil; la web la muestra.
   Backend de Farid (rama main):
     GET /mechanics/me/work-orders         -> órdenes activas del mecánico, EN_PROCESO o ESPERA_APROBACION (TEC3.1)
       [{ idOrden, idCita, ticket, estado, fechaInicio, servicio, placa }]
     GET /work-orders/{idOrden}/checklist  -> checklist de recepción; 404 si aún no se registra (T42.1)
       { idChecklist, idOrden, fechaRegistro, observacionGeneral, items: [{ elemento, estado OK|OBSERVADO, observacion }] }
   Pendientes en el backend (se usan apenas existan; mientras tanto la pantalla los muestra vacíos):
     GET /appointments/{idCita}/work-order -> orden de una cita ya finalizada (no sale en la lista de activas)
     GET /work-orders/{idOrden}/deficiencies -> deficiencias con sus fotos (T43.1 y T44.1)
       [{ idDeficiencia, categoria, severidad LEVE|MODERADA|CRITICA, descripcion, fechaRegistro, fotos: [{ idFoto, url }] }]
     GET /work-orders/photos/{idFoto}      -> la imagen (JPG o PNG), con el token JWT
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
  const url = f.url ?? (idFoto !== null ? `/work-orders/photos/${idFoto}` : "");
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
    const promesa = apiArchivo(foto.url).then((blob) => URL.createObjectURL(blob));
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
