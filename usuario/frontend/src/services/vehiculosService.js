import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Vehículos del cliente (panel web del cliente). Backend de Farid (rama main, T07.1 a T09.1):
     GET    /vehicles       -> [{ idVehiculo, placa, marca, modelo, anio, kilometraje, idCliente, nombreCliente }]
     POST   /vehicles       { placa, marca, modelo, anio, kilometraje } -> el vehículo registrado
            placa: formato ABC-123 (3 letras o números, guion y 3 números); 409 si ya está registrada en el sistema
     PUT    /vehicles/{id}  { marca, modelo, anio, kilometraje } -> actualizado. La placa no se puede cambiar
            y el kilometraje no puede ser menor al registrado.
     DELETE /vehicles/{id}  -> 204. Lo quita de la lista del cliente (el taller conserva su historial).
   El backend solo valida marca, modelo y año al editar: el formulario los exige siempre.
   Con VITE_DATOS_PRUEBA=true todo sale de datosPrueba.js. */

export const PATRON_PLACA = /^[A-Z0-9]{3}-[0-9]{3}$/;
export const ANIO_MINIMO = 1980;
export const KM_MAXIMO = 2000000;
export const MAX_TEXTO = 50;

export const MARCAS = [
  "Toyota", "Hyundai", "Kia", "Nissan", "Chevrolet", "Suzuki", "Mitsubishi", "Honda", "Mazda", "Volkswagen",
  "Ford", "Renault", "Peugeot", "Subaru", "JAC", "Changan", "Great Wall", "BYD", "Chery", "Geely",
];

const numero = (n) => {
  const v = Number(n);
  return Number.isFinite(v) ? v : 0;
};

const normalizar = (v = {}) => ({
  id: v.idVehiculo ?? v.id,
  placa: String(v.placa ?? "").toUpperCase(),
  marca: v.marca ?? "",
  modelo: v.modelo ?? "",
  anio: v.anio ?? null,
  kilometraje: numero(v.kilometraje),
});

// "abc123" -> "ABC-123" mientras se escribe
export function formatearPlaca(texto) {
  const limpio = String(texto ?? "").toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 6);
  return limpio.length > 3 ? `${limpio.slice(0, 3)}-${limpio.slice(3)}` : limpio;
}

export async function listarVehiculos() {
  const datos = prueba.DATOS_PRUEBA ? await prueba.listarVehiculosCliente() : await apiFetch("/vehicles");
  return (Array.isArray(datos) ? datos : []).map(normalizar).sort((a, b) => a.placa.localeCompare(b.placa));
}

const cuerpo = ({ marca, modelo, anio, kilometraje }) => ({
  marca: marca.trim(),
  modelo: modelo.trim(),
  anio: Number(anio),
  kilometraje: Number(kilometraje || 0),
});

export async function registrarVehiculo(datos) {
  const c = { placa: formatearPlaca(datos.placa), ...cuerpo(datos) };
  const r = prueba.DATOS_PRUEBA ? await prueba.registrarVehiculoCliente(c) : await apiFetch("/vehicles", { metodo: "POST", cuerpo: c });
  return normalizar(r);
}

export async function editarVehiculo(id, datos) {
  const c = cuerpo(datos);
  const r = prueba.DATOS_PRUEBA ? await prueba.editarVehiculoCliente(id, c) : await apiFetch(`/vehicles/${id}`, { metodo: "PUT", cuerpo: c });
  return normalizar(r);
}

export async function eliminarVehiculo(id) {
  if (prueba.DATOS_PRUEBA) return prueba.eliminarVehiculoCliente(id);
  await apiFetch(`/vehicles/${id}`, { metodo: "DELETE" });
}

export const formatoKm = (n) => `${Number(n || 0).toLocaleString("es-PE")} km`;
