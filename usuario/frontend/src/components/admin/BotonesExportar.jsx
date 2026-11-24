import { useState } from "react";
import { descargarReporte, FORMATOS } from "../../services/reportesService";
import "./BotonesExportar.css";

/* T74.2 – Botones "Excel" y "PDF" que descargan el reporte con los filtros que tiene la pantalla.
   tipo: pagos | citas | metricas · filtros: los mismos parámetros que usa la pantalla
   alResultado(tipo, texto): avisa a la pantalla si se descargó o si falló. */
function BotonesExportar({ tipo, filtros, deshabilitado = false, alResultado }) {
  const [descargando, setDescargando] = useState(null);

  const exportar = async (formato) => {
    setDescargando(formato);
    try {
      const nombre = await descargarReporte(tipo, formato, filtros);
      alResultado?.("ok", `Se descargó ${nombre}.`);
    } catch (e) {
      alResultado?.("error", `No se pudo exportar el reporte: ${e.message}`);
    } finally {
      setDescargando(null);
    }
  };

  return (
    <div className="exportar" role="group" aria-label="Exportar reporte">
      <span className="exportar-titulo">Exportar</span>
      {FORMATOS.map((f) => (
        <button
          key={f.valor}
          type="button"
          className={`btn btn-sec exportar-${f.valor}`}
          onClick={() => exportar(f.valor)}
          disabled={deshabilitado || descargando !== null}
          aria-label={`Exportar a ${f.texto}`}
        >
          {descargando === f.valor ? (
            <span className="btn-spinner" aria-hidden="true" />
          ) : (
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8Z" />
              <path d="M14 3v5h5M12 11v6M9.5 14.5 12 17l2.5-2.5" />
            </svg>
          )}
          {f.texto}
        </button>
      ))}
    </div>
  );
}

export default BotonesExportar;
