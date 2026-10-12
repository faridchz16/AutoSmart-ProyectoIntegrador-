// Datos de prueba para el panel mientras el backend no tenga los endpoints.
// Se activan con VITE_DATOS_PRUEBA=true en el archivo .env (activos por defecto).
// Para usar el backend real: VITE_DATOS_PRUEBA=false
// Los cambios (crear, activar, desactivar) se pierden al recargar la página.

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
