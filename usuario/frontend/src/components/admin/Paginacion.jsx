import "./admin-ui.css";

// Números a mostrar: todos si son pocos; si no, 1 … actual-1 actual actual+1 … última
function numeros(actual, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);
  const lista = new Set([0, total - 1, actual - 1, actual, actual + 1]);
  const ordenados = [...lista].filter((n) => n >= 0 && n < total).sort((a, b) => a - b);
  const salida = [];
  ordenados.forEach((n, i) => {
    if (i > 0 && n - ordenados[i - 1] > 1) salida.push(`…${n}`);
    salida.push(n);
  });
  return salida;
}

const Flecha = ({ izquierda }) => (
  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d={izquierda ? "m15 6-6 6 6 6" : "m9 6 6 6-6 6"} />
  </svg>
);

function Paginacion({ pagina, paginas, total, tamano, alCambiar }) {
  if (total === 0) return null;
  const desde = pagina * tamano + 1;
  const hasta = Math.min(total, desde + tamano - 1);

  return (
    <nav className="paginacion" aria-label="Paginación">
      <p className="paginacion-info">
        Mostrando <strong>{desde}–{hasta}</strong> de <strong>{total}</strong>
      </p>
      <div className="paginacion-botones">
        <button type="button" onClick={() => alCambiar(pagina - 1)} disabled={pagina === 0} aria-label="Página anterior">
          <Flecha izquierda />
        </button>
        {numeros(pagina, paginas).map((n) =>
          typeof n === "string" ? (
            <span key={n} className="paginacion-puntos">…</span>
          ) : (
            <button
              type="button"
              key={n}
              className={n === pagina ? "actual" : ""}
              aria-current={n === pagina ? "page" : undefined}
              onClick={() => alCambiar(n)}
            >
              {n + 1}
            </button>
          )
        )}
        <button type="button" onClick={() => alCambiar(pagina + 1)} disabled={pagina >= paginas - 1} aria-label="Página siguiente">
          <Flecha />
        </button>
      </div>
    </nav>
  );
}

export default Paginacion;
