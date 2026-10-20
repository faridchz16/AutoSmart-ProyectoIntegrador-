import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Perfil profesional del mecánico (T13.2, T14.1) – backend de Farid (rama main):
     GET /mechanics/me  -> { idUsuario, nombre, correo, aniosExperiencia, especialidades: [String] }
     PUT /mechanics/me  { aniosExperiencia, especialidades }  -> mismo formato
   Si el mecánico aún no completó su perfil, el GET devuelve 0 años y sin especialidades.
   Reglas del backend: años entre 0 y 60, hasta 10 especialidades de máximo 60 caracteres.
   Las especialidades nuevas se crean solas; el backend ignora mayúsculas al compararlas. */

const RUTA = "/mechanics/me";

export const MAX_ANIOS = 60;
export const MAX_ESPECIALIDADES = 10;
export const MAX_LARGO_ESPECIALIDAD = 60;

// Sugerencias comunes en un taller (se suman a las especialidades que piden los servicios)
const SUGERENCIAS_BASE = [
  "Mecánica general",
  "Frenos",
  "Suspensión",
  "Motor",
  "Electricidad automotriz",
  "Diagnóstico con escáner",
  "Alineamiento y balanceo",
  "Transmisión",
  "Aire acondicionado",
  "Inyección electrónica",
];

function normalizar(p = {}) {
  return {
    nombre: p.nombre ?? "",
    correo: p.correo ?? "",
    aniosExperiencia: Number(p.aniosExperiencia ?? 0),
    especialidades: Array.isArray(p.especialidades) ? p.especialidades : [],
  };
}

export async function obtenerPerfil() {
  const datos = prueba.DATOS_PRUEBA ? await prueba.obtenerPerfilMecanico() : await apiFetch(RUTA);
  return normalizar(datos);
}

export async function guardarPerfil({ aniosExperiencia, especialidades }) {
  const cuerpo = { aniosExperiencia, especialidades };
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.guardarPerfilMecanico(cuerpo)
    : await apiFetch(RUTA, { metodo: "PUT", cuerpo });
  return normalizar(datos);
}

// Especialidades sugeridas: las que exigen los servicios del catálogo + las comunes, sin repetir
export async function listarSugerencias() {
  let deServicios = [];
  try {
    const datos = prueba.DATOS_PRUEBA ? [] : await apiFetch("/services");
    const lista = Array.isArray(datos) ? datos : datos.contenido ?? datos.content ?? [];
    deServicios = lista.map((s) => s.especialidad).filter(Boolean);
  } catch {
    // Si el catálogo falla, bastan las sugerencias comunes
  }
  const vistas = new Set();
  return [...deServicios, ...SUGERENCIAS_BASE].filter((n) => {
    const clave = n.trim().toLowerCase();
    if (vistas.has(clave)) return false;
    vistas.add(clave);
    return true;
  });
}

// Primera letra en mayúscula, igual que hace el backend al crear una especialidad nueva
export const capitalizar = (t) => (t ? t.charAt(0).toUpperCase() + t.slice(1) : t);

export const mismaEspecialidad = (a, b) => a.trim().toLowerCase() === b.trim().toLowerCase();
