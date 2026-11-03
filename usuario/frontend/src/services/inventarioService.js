import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Inventario del almacén (T57.2). Backend de Farid: T57.1 (rama feature/backend-inventory).
   Contrato propuesto (el backend aún no está en main):
     GET   /admin/materials               -> todos los materiales, activos e inactivos
     POST  /admin/materials               -> crea un material; 409 si el código ya existe
     PUT   /admin/materials/{id}          -> edita datos, costo y stock mínimo (el stock NO se edita aquí)
     PATCH /admin/materials/{id}/estado   -> { activo } activa o desactiva sin borrar
   Cuerpo:    { codigo, nombre, descripcion, categoria, unidad, costoUnitario, stockMinimo }
   Respuesta: { idMaterial, codigo, nombre, descripcion, categoria, unidad, costoUnitario,
                stockActual, stockMinimo, activo }
   Reglas: código único (se guarda en mayúsculas), costo ≥ 0, stock mínimo entero ≥ 0.
   Un material nuevo empieza con stock 0: el stock solo cambia con movimientos del kardex
   (ingresos T58, salidas por solicitudes aprobadas T59 y consumo en la orden T51).
   Con VITE_DATOS_PRUEBA=true todo sale de datosPrueba.js. */

export const CATEGORIAS_MATERIAL = {
  REPUESTO: "Repuestos",
  FRENOS: "Frenos",
  FILTRO: "Filtros",
  LUBRICANTE: "Lubricantes y fluidos",
  ELECTRICO: "Eléctricos",
  NEUMATICO: "Neumáticos",
  INSUMO: "Insumos",
};

// Unidad de medida: texto en singular y plural
export const UNIDADES = {
  UNIDAD: ["unidad", "unidades"],
  JUEGO: ["juego", "juegos"],
  LITRO: ["litro", "litros"],
  GALON: ["galón", "galones"],
  KIT: ["kit", "kits"],
  METRO: ["metro", "metros"],
};

export const textoUnidad = (unidad, cantidad = 2) => {
  const [uno, varios] = UNIDADES[unidad] ?? UNIDADES.UNIDAD;
  return cantidad === 1 ? uno : varios;
};

// "fre-001 " -> "FRE-001"
export const normalizarCodigo = (codigo) => String(codigo ?? "").trim().toUpperCase();

const codigoDe = (texto, lista, porDefecto) => {
  const valor = String(texto ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .trim()
    .toUpperCase();
  return lista[valor] ? valor : porDefecto;
};

const numero = (n, porDefecto = 0) => {
  const v = Number(n);
  return Number.isFinite(v) ? v : porDefecto;
};

function normalizarMaterial(m = {}) {
  return {
    id: m.idMaterial ?? m.id,
    codigo: normalizarCodigo(m.codigo),
    nombre: m.nombre ?? "",
    descripcion: m.descripcion ?? "",
    categoria: codigoDe(m.categoria, CATEGORIAS_MATERIAL, "REPUESTO"),
    unidad: codigoDe(m.unidad ?? m.unidadMedida, UNIDADES, "UNIDAD"),
    costoUnitario: numero(m.costoUnitario ?? m.costo),
    stockActual: numero(m.stockActual ?? m.stock),
    stockMinimo: numero(m.stockMinimo),
    activo: m.activo ?? true,
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
  return normalizarMaterial({ stockActual: 0, activo: true, ...material, ...datos });
}

export async function editarMaterial(id, material) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.editarMaterial(id, material)
    : await apiFetch(`/admin/materials/${id}`, { metodo: "PUT", cuerpo: material });
  return normalizarMaterial({ idMaterial: id, ...material, ...datos });
}

export async function cambiarEstadoMaterial(id, activo) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.cambiarEstadoMaterial(id, activo)
    : await apiFetch(`/admin/materials/${id}/estado`, { metodo: "PATCH", cuerpo: { activo } });
  return { activo: datos?.activo ?? activo };
}

// Formatos
const SOLES = new Intl.NumberFormat("es-PE", { style: "currency", currency: "PEN" });
const CANTIDAD = new Intl.NumberFormat("es-PE", { maximumFractionDigits: 2 });
export const formatoSoles = (n) => SOLES.format(n);
export const formatoCantidad = (n) => CANTIDAD.format(n);
