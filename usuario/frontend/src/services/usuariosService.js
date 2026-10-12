import { apiFetch } from "./api";
import { normalizarRol } from "./sesion";
import * as prueba from "./datosPrueba";

/* Gestión de usuarios del panel de administración.
   Endpoints según el backlog del Sprint 1 (Farid):
     GET   /admin/users?rol=&q=&page=&size=   -> lista paginada (T16.1)
     POST  /admin/mechanics                    -> crea un mecánico (T15.1)
     PATCH /admin/users/{id}/estado            -> activa o desactiva (T17.1)
   Si el backend usa otros nombres, solo hay que cambiarlos aquí. */

export const NOMBRE_ROL = {
  CLIENTE: "Cliente",
  MECANICO: "Mecánico",
  ADMIN: "Administrador",
};

// Adapta lo que devuelva el backend a un formato único para la pantalla
function normalizarUsuario(u = {}) {
  const rol = u.rol && typeof u.rol === "object" ? u.rol.nombre : u.rol;
  return {
    id: u.id ?? u.idUsuario ?? u.id_usuario,
    nombre: u.nombre ?? "",
    correo: u.correo ?? "",
    rol: normalizarRol(rol),
    activo: u.activo ?? true,
    creadoEn: u.creadoEn ?? u.creado_en ?? null,
  };
}

// Acepta una página de Spring ({ content, totalElements, ... }) o una lista simple
function leerPagina(datos, { rol, q, pagina, tamano }) {
  if (Array.isArray(datos)) {
    const texto = q.trim().toLowerCase();
    const todos = datos
      .map(normalizarUsuario)
      .filter((u) => !rol || u.rol === rol)
      .filter((u) => !texto || `${u.nombre} ${u.correo}`.toLowerCase().includes(texto));
    return {
      usuarios: todos.slice(pagina * tamano, pagina * tamano + tamano),
      total: todos.length,
      paginas: Math.max(1, Math.ceil(todos.length / tamano)),
    };
  }
  const usuarios = (datos.content ?? []).map(normalizarUsuario);
  return {
    usuarios,
    total: datos.totalElements ?? usuarios.length,
    paginas: Math.max(1, datos.totalPages ?? 1),
  };
}

export async function listarUsuarios({ rol = "", q = "", pagina = 0, tamano = 10 } = {}) {
  const filtros = { rol, q, pagina, tamano };
  if (prueba.DATOS_PRUEBA) {
    return leerPagina(await prueba.listarUsuarios(filtros), filtros);
  }
  const params = new URLSearchParams({ page: pagina, size: tamano });
  if (rol) params.set("rol", rol);
  if (q.trim()) params.set("q", q.trim());
  return leerPagina(await apiFetch(`/admin/users?${params}`), filtros);
}

// Devuelve el mecánico creado y la contraseña temporal para entregársela
export async function crearMecanico({ nombre, correo, password }) {
  const cuerpo = { nombre, correo, password };
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.crearMecanico(cuerpo)
    : await apiFetch("/admin/mechanics", { metodo: "POST", cuerpo });
  return {
    usuario: normalizarUsuario(datos.usuario ?? datos),
    passwordTemporal: datos.passwordTemporal ?? password,
  };
}

export async function cambiarEstadoUsuario(id, activo) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.cambiarEstadoUsuario(id, activo)
    : await apiFetch(`/admin/users/${id}/estado`, { metodo: "PATCH", cuerpo: { activo } });
  return { activo: datos?.activo ?? activo };
}
