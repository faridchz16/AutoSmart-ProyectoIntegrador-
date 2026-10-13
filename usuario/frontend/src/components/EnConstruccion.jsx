/* Marcador temporal para pantallas del panel que aún se están construyendo.
   Se reemplaza por la pantalla real (tabla, formulario, etc.). */
function EnConstruccion({ titulo, texto }) {
  return (
    <div className="adm-vacio">
      <span className="adm-vacio-icono">
        <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M5.6 18.4l2.1-2.1M16.3 7.7l2.1-2.1" />
        </svg>
      </span>
      <span className="adm-etiqueta">En construcción</span>
      <h2>{titulo}</h2>
      <p>{texto}</p>
    </div>
  );
}

export default EnConstruccion;
