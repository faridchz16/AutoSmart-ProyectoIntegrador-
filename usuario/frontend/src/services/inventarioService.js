import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Inventario del almacén (T57.2). Backend de Farid (rama main, T57.1):
     GET  /admin/materials          -> todos los materiales, ordenados por nombre
          ?buscar=texto              (opcional) filtra por código o nombre
          ?soloBajoStock=true        (opcional) solo los que tienen stock ≤ mínimo (para T60.1)
     GET  /admin/materials/{id}     -> un material; 404 si no existe
     POST /admin/materials          -> crea; 201. 409 si el código ya existe
     PUT  /admin/materials/{id}     -> edita; 409 si el código lo usa otro material
   Cuerpo:    { codigo, nombre, unidad, costoUnitario, stockMinimo }
   Respuesta: { idMaterial, codigo, nombre, unidad, costoUnitario, stockActual, stockMinimo, bajoStock }
   Reglas del backend: código único (lo guarda en mayúsculas, máx. 30), nombre máx. 100,
   unidad de texto libre (la guarda en minúsculas, máx. 20), costo ≥ 0 (2 decimales), stock mínimo entero ≥ 0.
   Ingresos y kardex (T58.1):
     POST /admin/materials/{id}/entries { cantidad (1 a 100000), costoUnitario (opcional, ≥ 0: actualiza el costo
          del material), referencia (opcional, máx. 200) } -> el movimiento de ENTRADA; la fecha la pone el servidor
     GET  /admin/materials/{id}/kardex -> movimientos del material, del más antiguo al más nuevo
     Movimiento: { idMovimiento, idMaterial, codigoMaterial, tipo ENTRADA|SALIDA, cantidad, costoUnitario,
                   stockResultante, referencia, idOrden, registradoPor, fecha }
   Un material nuevo empieza con stock 0: el stock solo cambia con el kardex
   (ingresos T58, salidas por solicitudes aprobadas T59 y consumo en la orden T51).
   Con VITE_DATOS_PRUEBA=true todo sale de datosPrueba.js. */

export const MAX_CODIGO = 30;
export const MAX_NOMBRE = 100;

// Unidades que ofrece el formulario: lo que se guarda (singular) y su plural
export const UNIDADES = [
  ["unidad", "unidades"],
  ["juego", "juegos"],
  ["litro", "litros"],
  ["galón", "galones"],
  ["kit", "kits"],
  ["metro", "metros"],
];

const sinTildes = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "");

// La unidad llega como texto libre ("unidad", "GALON", "caja"): se muestra en singular o plural
export function textoUnidad(unidad, cantidad = 2) {
  const u = String(unidad ?? "").trim().toLowerCase() || "unidad";
  const conocida = UNIDADES.find(([uno]) => sinTildes(uno) === sinTildes(u));
  if (conocida) return cantidad === 1 ? conocida[0] : conocida[1];
  if (cantidad === 1) return u;
  return /[aeiouáéó]$/.test(u) ? `${u}s` : `${u}es`;
}

// Unidad tal como se elige en el formulario ("GALON" o "galon" -> "galón")
export function unidadDelFormulario(unidad) {
  const u = String(unidad ?? "").trim().toLowerCase();
  return UNIDADES.find(([uno]) => sinTildes(uno) === sinTildes(u))?.[0] ?? u;
}

// "fre-001 " -> "FRE-001"
export const normalizarCodigo = (codigo) => String(codigo ?? "").trim().toUpperCase();

const numero = (n) => {
  const v = Number(n);
  return Number.isFinite(v) ? v : 0;
};

function normalizarMaterial(m = {}) {
  const stockActual = numero(m.stockActual ?? m.stock);
  const stockMinimo = numero(m.stockMinimo);
  return {
    id: m.idMaterial ?? m.id,
    codigo: normalizarCodigo(m.codigo),
    nombre: m.nombre ?? "",
    unidad: unidadDelFormulario(m.unidad) || "unidad",
    costoUnitario: numero(m.costoUnitario ?? m.costo),
    stockActual,
    stockMinimo,
    bajoStock: m.bajoStock ?? stockActual <= stockMinimo,
  };
}

const aLista = (datos) => (Array.isArray(datos) ? datos : datos?.contenido ?? datos?.content ?? []);

export async function listarMateriales() {
  try {
    const datos = prueba.DATOS_PRUEBA ? await prueba.listarMateriales() : await apiFetch("/admin/materials");
    return aLista(datos).map(normalizarMaterial);
  } catch (e) {
    if (e.status === 404 || e.status === 405) {
      throw new Error("El servidor todavía no tiene el inventario (falta GET /admin/materials del backend).", {
        cause: e,
      });
    }
    throw e;
  }
}

export async function crearMaterial(material) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.crearMaterial(material)
    : await apiFetch("/admin/materials", { metodo: "POST", cuerpo: material });
  return normalizarMaterial({ stockActual: 0, ...material, ...datos });
}

export async function editarMaterial(id, material) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.editarMaterial(id, material)
    : await apiFetch(`/admin/materials/${id}`, { metodo: "PUT", cuerpo: material });
  return normalizarMaterial({ idMaterial: id, ...material, ...datos });
}

/* ---------- Ingresos de mercadería y kardex (T58.2) ---------- */

export const MAX_REFERENCIA = 200;
export const MAX_CANTIDAD_INGRESO = 100000;

function normalizarMovimiento(m = {}) {
  return {
    id: m.idMovimiento ?? m.id,
    tipo: String(m.tipo ?? "").toUpperCase() === "SALIDA" ? "SALIDA" : "ENTRADA",
    cantidad: numero(m.cantidad),
    costoUnitario: numero(m.costoUnitario),
    stockResultante: numero(m.stockResultante),
    referencia: m.referencia ?? "",
    idOrden: m.idOrden ?? null,
    registradoPor: m.registradoPor ?? "",
    fecha: m.fecha ?? "",
  };
}

// Proveedor, documento y fecha de recepción van en la referencia (el backend no tiene campos propios)
export function armarReferencia({ proveedor, documento, fechaRecepcion, hoy }) {
  return [
    proveedor.trim() && `Proveedor: ${proveedor.trim()}`,
    documento.trim() && `Doc.: ${documento.trim()}`,
    fechaRecepcion && fechaRecepcion !== hoy && `Recibido el ${fechaRecepcion.split("-").reverse().join("/")}`,
  ]
    .filter(Boolean)
    .join(" · ");
}

export async function registrarIngreso(idMaterial, { cantidad, costoUnitario, referencia }) {
  const cuerpo = { cantidad: Number(cantidad), referencia: referencia || null };
  if (costoUnitario !== null && costoUnitario !== undefined) cuerpo.costoUnitario = Number(costoUnitario);
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.registrarIngreso(idMaterial, cuerpo)
    : await apiFetch(`/admin/materials/${idMaterial}/entries`, { metodo: "POST", cuerpo });
  return normalizarMovimiento(datos);
}

// Del más nuevo al más antiguo
export async function obtenerKardex(idMaterial) {
  const datos = prueba.DATOS_PRUEBA ? await prueba.obtenerKardex(idMaterial) : await apiFetch(`/admin/materials/${idMaterial}/kardex`);
  return (Array.isArray(datos) ? datos : []).map(normalizarMovimiento).reverse();
}

// "2026-11-09T22:05:13" -> "09/11/2026 22:05"
export const fechaHora = (f) => (f ? `${f.slice(8, 10)}/${f.slice(5, 7)}/${f.slice(0, 4)} ${f.slice(11, 16)}` : "—");

// Formatos
const SOLES = new Intl.NumberFormat("es-PE", { style: "currency", currency: "PEN" });
const CANTIDAD = new Intl.NumberFormat("es-PE", { maximumFractionDigits: 2 });
export const formatoSoles = (n) => SOLES.format(n);
export const formatoCantidad = (n) => CANTIDAD.format(n);
