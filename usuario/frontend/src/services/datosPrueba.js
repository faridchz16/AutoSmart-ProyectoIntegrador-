// Datos de prueba para el panel mientras el backend no tenga los endpoints.
// Se activan con VITE_DATOS_PRUEBA=true en el archivo .env (activos por defecto).
// Para usar el backend real: VITE_DATOS_PRUEBA=false
// Los cambios (crear, editar, activar, desactivar) se pierden al recargar la página.

export const DATOS_PRUEBA = import.meta.env.VITE_DATOS_PRUEBA !== "false";

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

export async function crearMecanico({ nombre, correo, password }) {
  await esperar(500);
  if (usuarios.some((u) => u.correo.toLowerCase() === correo.toLowerCase())) {
    throw new Error("Ya existe una cuenta con ese correo.");
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
  return { ...nuevo, passwordTemporal: password };
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
  ["Cambio de aceite y filtro", "Aceite de motor según especificación del fabricante y cambio de filtro.", 120, 45, true],
  ["Alineamiento y balanceo", "Ajuste de ángulos de dirección y balanceo de las cuatro ruedas.", 90, 60, true],
  ["Revisión de frenos", "Inspección de pastillas, discos, líquido y mangueras de freno.", 80, 60, true],
  ["Diagnóstico con escáner", "Lectura de códigos de falla de la computadora del vehículo.", 70, 40, true],
  ["Revisión de batería", "Prueba de carga, bornes y sistema de arranque.", 30, 20, true],
  ["Cambio de pastillas de freno", "Reemplazo de pastillas delanteras o posteriores.", 150, 90, true],
  ["Afinamiento de motor", "Bujías, filtros de aire y combustible, limpieza de inyectores.", 220, 120, true],
  ["Cambio de refrigerante", "Drenado y llenado del sistema de enfriamiento.", 95, 45, true],
  ["Revisión de suspensión", "Inspección de amortiguadores, rótulas y terminales.", 110, 75, false],
  ["Lavado de motor", "Limpieza exterior del compartimiento del motor.", 50, 40, false],
].map(([nombre, descripcion, precioBase, duracionMinutos, activo], i) => ({
  id: i + 1,
  nombre,
  descripcion,
  precioBase,
  duracionMinutos,
  activo,
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
