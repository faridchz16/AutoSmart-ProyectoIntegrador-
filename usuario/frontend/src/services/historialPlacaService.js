import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Historial de mantenimiento de cualquier vehículo por placa (solo ADMIN). Backend de Farid (rama main, T72.1):
     GET /admin/vehicles/{placa}/history
       -> { idVehiculo, placa, marca, modelo, anio, kilometraje, propietario { nombre, correo },
            totalMantenimientos,
            mantenimientos: [{ idOrden, idCita, ticket, estado, servicio, mecanico, fechaInicio, fechaFin,
                               diagnostico, costoRepuestos }] }   (del más nuevo al más antiguo)
       404 si no existe un vehículo con esa placa (no distingue mayúsculas).
   Lo usan el buscador por placa (T72.2) y el cobro en efectivo (T69.2), que necesita el número de la orden.
   Con VITE_DATOS_PRUEBA=true sale de datosPrueba.js. */

const numero = (n) => {
  const v = Number(n);
  return Number.isFinite(v) ? v : 0;
};

// "abc 123" -> "ABC-123" (solo para mostrar la búsqueda; el backend compara tal cual sin distinguir mayúsculas)
export const normalizarPlaca = (placa) => String(placa ?? "").trim().toUpperCase();

function normalizarMantenimiento(m = {}) {
  return {
    idOrden: m.idOrden,
    idCita: m.idCita,
    ticket: m.ticket ?? "",
    estado: String(m.estado ?? "").toUpperCase(),
    servicio: m.servicio ?? "Servicio",
    mecanico: m.mecanico ?? "",
    fechaInicio: m.fechaInicio ?? "",
    fechaFin: m.fechaFin ?? "",
    diagnostico: (m.diagnostico ?? "").trim(),
    costoRepuestos: numero(m.costoRepuestos),
  };
}

export async function obtenerHistorialPlaca(placa) {
  const buscada = normalizarPlaca(placa);
  try {
    const datos = prueba.DATOS_PRUEBA
      ? await prueba.obtenerHistorialPlaca(buscada)
      : await apiFetch(`/admin/vehicles/${encodeURIComponent(buscada)}/history`);
    const mantenimientos = (Array.isArray(datos?.mantenimientos) ? datos.mantenimientos : []).map(normalizarMantenimiento);
    return {
      idVehiculo: datos?.idVehiculo,
      placa: datos?.placa ?? buscada,
      marca: datos?.marca ?? "",
      modelo: datos?.modelo ?? "",
      anio: datos?.anio ?? null,
      kilometraje: datos?.kilometraje ?? null,
      propietario: { nombre: datos?.propietario?.nombre ?? "", correo: datos?.propietario?.correo ?? "" },
      totalMantenimientos: numero(datos?.totalMantenimientos ?? mantenimientos.length),
      mantenimientos,
    };
  } catch (e) {
    if (e.status === 404) {
      const error = new Error(`No hay ningún vehículo registrado con la placa ${buscada}.`, { cause: e });
      error.status = 404;
      throw error;
    }
    throw e;
  }
}
