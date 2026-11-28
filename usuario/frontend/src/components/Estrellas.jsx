import "./Estrellas.css";

/* Estrellas de calificación (1 a 5) de solo lectura. Acepta promedios con decimales (4.6 = cuatro estrellas
   y 60 % de la quinta). Se usa en el panel del mecánico (T67.2, T68.2) y del administrador (T71.2, T73.2). */

const RUTA = "M12 2.8l2.8 5.7 6.3.9-4.55 4.43 1.07 6.27L12 17.15 6.38 20.1l1.07-6.27L2.9 9.4l6.3-.9z";

function Estrellas({ valor = 0, tamano = 16, etiqueta }) {
  const v = Math.max(0, Math.min(5, Number(valor) || 0));
  const texto = etiqueta ?? `${v.toLocaleString("es-PE", { maximumFractionDigits: 1 })} de 5 estrellas`;
  return (
    <span className="estrellas" role="img" aria-label={texto} style={{ "--tam": `${tamano}px` }}>
      {[0, 1, 2, 3, 4].map((i) => {
        const lleno = Math.max(0, Math.min(1, v - i));
        return (
          <span key={i} className="estrella" aria-hidden="true">
            <svg viewBox="0 0 24 24" className="estrella-fondo">
              <path d={RUTA} />
            </svg>
            {lleno > 0 && (
              <span className="estrella-lleno" style={{ width: `${lleno * 100}%` }}>
                <svg viewBox="0 0 24 24">
                  <path d={RUTA} />
                </svg>
              </span>
            )}
          </span>
        );
      })}
    </span>
  );
}

export default Estrellas;
