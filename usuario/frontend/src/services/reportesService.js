import { descargarArchivo, guardarBlob } from "./api";
import * as prueba from "./datosPrueba";

/* Exportación de reportes del administrador (T74.2). Backend de Farid (rama main, T74.1):
     GET /admin/reports/{tipo}?formato=xlsx|pdf&...filtros   -> el archivo (Content-Disposition con su nombre)
       pagos:    desde, hasta, metodo, estado   (sin fechas: el mes actual)
       citas:    desde, hasta, estado           (sin fechas: el mes actual)
       metricas: mes (AAAA-MM; sin mes: el actual)
       Usa los mismos filtros que la pantalla, así el archivo trae lo mismo que se ve.
       400 si el tipo, el formato o un filtro no son válidos.
   Con VITE_DATOS_PRUEBA=true se descarga un archivo de ejemplo. */

export const FORMATOS = [
  { valor: "xlsx", texto: "Excel" },
  { valor: "pdf", texto: "PDF" },
];

export async function descargarReporte(tipo, formato, filtros = {}) {
  const params = new URLSearchParams({ formato });
  Object.entries(filtros).forEach(([clave, valor]) => {
    if (valor) params.set(clave, valor);
  });
  if (prueba.DATOS_PRUEBA) {
    const { blob, nombre } = await prueba.reportePrueba(tipo, formato, filtros);
    guardarBlob(blob, nombre);
    return nombre;
  }
  return descargarArchivo(`/admin/reports/${tipo}?${params}`, `reporte-${tipo}.${formato}`);
}
