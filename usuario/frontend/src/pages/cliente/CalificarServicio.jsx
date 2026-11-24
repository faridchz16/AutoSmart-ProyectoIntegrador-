import { useState } from "react";
import { calificar, calificacionGuardada, MAX_COMENTARIO, TEXTO_ESTRELLAS } from "../../services/mantenimientoClienteService";
import Estrellas from "../../components/Estrellas";
import { fechaHora } from "../../utils/fechas";

/* Calificar la atención del mecánico (CLI-07, API de T65.1): 1 a 5 estrellas y un comentario opcional.
   Una sola vez por servicio. Las estrellas son botones de opción (se eligen con el teclado o con el mouse). */

const RUTA = "M12 2.8l2.8 5.7 6.3.9-4.55 4.43 1.07 6.27L12 17.15 6.38 20.1l1.07-6.27L2.9 9.4l6.3-.9z";

function CalificarServicio({ idOrden, idCita, mecanico, alCalificar }) {
  const [guardada, setGuardada] = useState(() => calificacionGuardada(idCita));
  const [estrellas, setEstrellas] = useState(0);
  const [resaltada, setResaltada] = useState(0);
  const [comentario, setComentario] = useState("");
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);

  const nombre = mecanico.split(" ")[0] || "el mecánico";

  if (guardada) {
    return (
      <section id="calificar" className="seg-caja cal-hecha" aria-labelledby="cal-titulo">
        <h3 id="cal-titulo">Tu calificación</h3>
        {guardada.estrellas ? (
          <>
            <div className="cal-resultado">
              <Estrellas valor={guardada.estrellas} tamano={24} />
              <strong>{TEXTO_ESTRELLAS[guardada.estrellas]}</strong>
            </div>
            {guardada.comentario && <blockquote>«{guardada.comentario}»</blockquote>}
            <p className="seg-nota">
              Gracias por calificar a {mecanico}
              {guardada.fecha && ` el ${fechaHora(guardada.fecha)}`}. Tu opinión ayuda a otros clientes a elegir.
            </p>
          </>
        ) : (
          <p className="seg-nota">Ya calificaste este servicio. ¡Gracias por tu opinión!</p>
        )}
      </section>
    );
  }

  const enviar = async (e) => {
    e.preventDefault();
    if (!estrellas) return setError("Elige de 1 a 5 estrellas.");
    if (comentario.trim().length > MAX_COMENTARIO) return setError(`El comentario puede tener hasta ${MAX_COMENTARIO} caracteres.`);
    setEnviando(true);
    setError("");
    try {
      const r = await calificar({ idOrden, idCita }, { estrellas, comentario });
      setGuardada(r);
      alCalificar?.(r);
    } catch (err) {
      if (err.status === 409 && /calificado/i.test(err.message)) setGuardada({ estrellas: null });
      else setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  const vista = resaltada || estrellas;

  return (
    <section id="calificar" className="seg-caja cal" aria-labelledby="cal-titulo">
      <h3 id="cal-titulo">¿Cómo te atendió {nombre}?</h3>
      <p className="seg-nota cal-ayuda">Califica la atención de {mecanico}. Solo puedes hacerlo una vez.</p>
      <form onSubmit={enviar} noValidate>
        <fieldset className="cal-estrellas" onMouseLeave={() => setResaltada(0)} disabled={enviando}>
          <legend className="sr-only">Estrellas</legend>
          {[1, 2, 3, 4, 5].map((n) => (
            <label key={n} className={n <= vista ? "cal-estrella llena" : "cal-estrella"} onMouseEnter={() => setResaltada(n)}>
              <input
                type="radio"
                name="estrellas"
                value={n}
                checked={estrellas === n}
                onChange={() => {
                  setEstrellas(n);
                  setError("");
                }}
              />
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d={RUTA} />
              </svg>
              <span className="sr-only">
                {n} {n === 1 ? "estrella" : "estrellas"}: {TEXTO_ESTRELLAS[n]}
              </span>
            </label>
          ))}
          <span className="cal-texto" aria-hidden="true">
            {vista ? TEXTO_ESTRELLAS[vista] : "Elige de 1 a 5"}
          </span>
        </fieldset>
        <div className="campo">
          <label htmlFor="cal-comentario">
            Comentario <span className="opcional">(opcional)</span>
          </label>
          <textarea
            id="cal-comentario"
            rows={3}
            maxLength={MAX_COMENTARIO}
            placeholder="Cuéntanos qué te pareció: puntualidad, trato, explicación del trabajo..."
            value={comentario}
            disabled={enviando}
            onChange={(e) => {
              setComentario(e.target.value);
              setError("");
            }}
          />
          <span className="campo-ayuda">
            {comentario.length}/{MAX_COMENTARIO}
          </span>
        </div>
        {error && (
          <p className="form-error-general" role="alert">
            {error}
          </p>
        )}
        <button type="submit" className="btn btn-pri cal-boton" disabled={enviando}>
          {enviando && <span className="btn-spinner" aria-hidden="true" />}
          {enviando ? "Enviando..." : "Enviar calificación"}
        </button>
      </form>
    </section>
  );
}

export default CalificarServicio;
