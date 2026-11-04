import { useRef, useState } from "react";
import { guardarDiagnostico, textoCategoria, SEVERIDADES, MIN_DIAGNOSTICO, MAX_DIAGNOSTICO } from "../../services/ordenesService";

/* T45.2 – Área de diagnóstico técnico en la vista web de la orden (API de T45.1).
   El mecánico explica la falla con sus palabras; el cliente lo leerá en la app.
   - Sin diagnóstico y con la orden EN_PROCESO: se muestra el formulario directamente
   - Con diagnóstico: se lee y se puede editar mientras la orden esté EN_PROCESO
   - En espera de aprobación o finalizada: solo lectura (el backend responde 409 si se intenta) */

const IconoEditar = () => (
  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M4 20h4L18.5 9.5a2.1 2.1 0 0 0-4-4L4 16v4Z" />
    <path d="m13.5 6.5 4 4" />
  </svg>
);

// "• Suspensión (crítica): Amortiguador delantero izquierdo con fuga…"
const baseDesdeDeficiencias = (deficiencias) =>
  deficiencias
    .map((d) => `• ${textoCategoria(d.categoria)} (${SEVERIDADES[d.severidad].texto.toLowerCase()}): ${d.descripcion}`)
    .join("\n");

function validar(texto) {
  const t = texto.trim();
  if (!t) return "Escribe el diagnóstico antes de guardarlo.";
  if (t.length < MIN_DIAGNOSTICO) return `Explica un poco más: mínimo ${MIN_DIAGNOSTICO} caracteres.`;
  if (t.length > MAX_DIAGNOSTICO) return `Máximo ${MAX_DIAGNOSTICO} caracteres.`;
  return "";
}

function DiagnosticoOrden({ idOrden, diagnostico, editable, estado, deficiencias, momento, alGuardar }) {
  const [editando, setEditando] = useState(false);
  const [borrador, setBorrador] = useState("");
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [recienGuardado, setRecienGuardado] = useState(false);
  const areaRef = useRef(null);

  // Sin diagnóstico y con la orden abierta, el formulario se muestra de una vez
  const mostrarFormulario = editable && (editando || !diagnostico);
  const largo = borrador.trim().length;
  const sinCambios = !!diagnostico && borrador.trim() === diagnostico.texto;

  const empezarEdicion = () => {
    setBorrador(diagnostico?.texto ?? "");
    setError("");
    setRecienGuardado(false);
    setEditando(true);
  };

  const cancelar = () => {
    setEditando(false);
    setBorrador("");
    setError("");
  };

  const guardar = async (e) => {
    e.preventDefault();
    const problema = validar(borrador);
    setError(problema);
    if (problema) return;
    setEnviando(true);
    try {
      const guardado = await guardarDiagnostico(idOrden, borrador);
      alGuardar(guardado);
      setEditando(false);
      setBorrador("");
      setRecienGuardado(true);
    } catch (err) {
      setError(
        err.status === 409
          ? `${err.message}. La orden ya no se puede editar: pulsa Actualizar para ver su estado.`
          : err.message
      );
    } finally {
      setEnviando(false);
    }
  };

  /* ----- Formulario (registrar o editar) ----- */
  if (mostrarFormulario) {
    return (
      <form className="ot-diag-form" onSubmit={guardar} noValidate>
        <div className="campo">
          <div className="campo-fila">
            <label htmlFor="ot-diag-texto">{diagnostico ? "Editar diagnóstico" : "¿Qué encontraste y qué hay que hacer?"}</label>
            <span className={largo > MAX_DIAGNOSTICO ? "contador excedido" : "contador"}>
              {largo}/{MAX_DIAGNOSTICO}
            </span>
          </div>
          <textarea
            id="ot-diag-texto"
            ref={areaRef}
            rows={5}
            value={borrador}
            placeholder="Ej. Amortiguador delantero izquierdo con fuga de aceite; se recomienda cambiar ambos delanteros. Pastillas al 20 %: cambiarlas en este servicio."
            onChange={(e) => {
              setBorrador(e.target.value);
              if (error) setError("");
            }}
            onKeyDown={(e) => {
              if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) guardar(e);
            }}
            aria-invalid={!!error}
            aria-describedby="ot-diag-ayuda"
          />
          <p id="ot-diag-ayuda" className={error ? "campo-error" : "campo-ayuda"} role={error ? "alert" : undefined}>
            {error || "Escríbelo en palabras simples: el cliente lo leerá en la app."}
          </p>
        </div>

        <div className="ot-diag-acciones">
          {deficiencias.length > 0 && !borrador.trim() && (
            <button
              type="button"
              className="ot-diag-base"
              onClick={() => {
                const base = `${baseDesdeDeficiencias(deficiencias)}\n\n`;
                setBorrador(base);
                // El cursor queda al final para seguir escribiendo la conclusión
                requestAnimationFrame(() => {
                  const area = areaRef.current;
                  if (!area) return;
                  area.focus();
                  area.setSelectionRange(base.length, base.length);
                  area.scrollTop = area.scrollHeight;
                });
              }}
            >
              Partir de las {deficiencias.length === 1 ? "deficiencia registrada" : `${deficiencias.length} deficiencias registradas`}
            </button>
          )}
          <div className="ot-diag-botones">
            {diagnostico && (
              <button type="button" className="btn btn-sec" onClick={cancelar} disabled={enviando}>
                Cancelar
              </button>
            )}
            <button type="submit" className="btn btn-pri" disabled={enviando || sinCambios}>
              {enviando && <span className="btn-spinner" aria-hidden="true" />}
              {enviando ? "Guardando..." : diagnostico ? "Guardar cambios" : "Guardar diagnóstico"}
            </button>
          </div>
        </div>
      </form>
    );
  }

  /* ----- Sin diagnóstico y sin poder editar ----- */
  if (!diagnostico) {
    return (
      <p className="ot-diag-vacio">
        {estado === "FINALIZADA" ? "Esta orden se cerró sin diagnóstico registrado." : "Aún no se registra el diagnóstico."}
      </p>
    );
  }

  /* ----- Lectura ----- */
  return (
    <div className="ot-diag">
      <p className="ot-diag-texto">{diagnostico.texto}</p>
      <div className="ot-diag-pie">
        <span className={recienGuardado ? "ot-diag-fecha ok" : "ot-diag-fecha"} role={recienGuardado ? "status" : undefined}>
          {recienGuardado ? "✓ Guardado " : "Registrado "}
          {diagnostico.fechaRegistro ? momento(diagnostico.fechaRegistro) : ""}
        </span>
        {editable ? (
          <button type="button" className="btn-icono" onClick={empezarEdicion}>
            <IconoEditar />
            <span>Editar</span>
          </button>
        ) : (
          <span className="ot-diag-bloqueo">
            {estado === "ESPERA_APROBACION" ? "No se edita mientras el cliente revisa el presupuesto." : "Orden cerrada: solo lectura."}
          </span>
        )}
      </div>
    </div>
  );
}

export default DiagnosticoOrden;
