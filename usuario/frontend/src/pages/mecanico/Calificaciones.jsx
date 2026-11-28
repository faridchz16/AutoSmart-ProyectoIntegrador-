import { useEffect, useState } from "react";
import {
  obtenerMisCalificaciones,
  distribucion,
  fechaCorta,
  formatoPromedio,
} from "../../services/calificacionesService";
import Estrellas from "../../components/Estrellas";
import "../../components/admin/admin-ui.css";
import "./Calificaciones.css";

/* T67.2 – "Mis calificaciones" del mecánico (API de T67.1).
   - Promedio, total de reseñas y cuántas hay de cada nota
   - Las barras de cada nota filtran la lista; también se puede ver solo las que tienen comentario
   - Los comentarios que el administrador ocultó no llegan del servidor, pero su nota sí cuenta */

const ORDENES = [
  { valor: "recientes", texto: "Más recientes" },
  { valor: "menor", texto: "Menor nota" },
];

const textoResenas = (n) => (n === 1 ? "1 reseña" : `${n} reseñas`);

function Calificaciones() {
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, datos: null, error: "" });
  const [nota, setNota] = useState(0); // 0 = todas
  const [soloComentarios, setSoloComentarios] = useState(false);
  const [orden, setOrden] = useState("recientes");

  const cargando = resultado.clave !== recarga;
  const primeraCarga = resultado.clave === null;

  useEffect(() => {
    let vigente = true;
    obtenerMisCalificaciones()
      .then((datos) => vigente && setResultado({ clave: recarga, datos, error: "" }))
      .catch((e) => vigente && setResultado({ clave: recarga, datos: null, error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recarga]);

  const datos = resultado.datos;
  const lista = datos?.calificaciones ?? [];
  const conteo = distribucion(lista);
  const conComentario = lista.filter((c) => c.comentario).length;

  const visibles = lista
    .filter((c) => (!nota || c.estrellas === nota) && (!soloComentarios || c.comentario))
    .sort((a, b) => (orden === "menor" ? a.estrellas - b.estrellas || b.fecha.localeCompare(a.fecha) : 0));

  const hayFiltro = nota > 0 || soloComentarios;
  const limpiar = () => {
    setNota(0);
    setSoloComentarios(false);
  };

  return (
    <section className="gestion cal">
      <div className="gestion-cabecera">
        <div>
          <h2>Mis calificaciones</h2>
          <p>Lo que opinan los clientes de los servicios que atendiste.</p>
        </div>
        <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
          Actualizar
        </button>
      </div>

      {resultado.error ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudieron cargar tus calificaciones</p>
            <p>{resultado.error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        </div>
      ) : primeraCarga ? (
        <div className="cal-grid" aria-busy="true">
          <div className="cal-resumen">
            <span className="esq esq-largo" />
            <span className="esq" />
          </div>
          <div className="cal-lista-caja">
            <p className="cal-cargando">Cargando calificaciones...</p>
          </div>
        </div>
      ) : datos.total === 0 ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">Aún no tienes calificaciones</p>
            <p>Cuando un cliente califique un servicio que atendiste, su opinión aparecerá aquí.</p>
          </div>
        </div>
      ) : (
        <div className={cargando ? "cal-grid actualizando" : "cal-grid"} aria-busy={cargando}>
          {/* ---------- Resumen ---------- */}
          <aside className="cal-resumen" aria-label="Resumen de tus calificaciones">
            <p className="cal-etiqueta">Tu promedio</p>
            <div className="cal-promedio">
              <strong>{formatoPromedio(datos.promedio)}</strong>
              <div>
                <Estrellas valor={datos.promedio} tamano={20} />
                <span>{textoResenas(datos.total)}</span>
              </div>
            </div>

            <ul className="cal-barras" aria-label="Filtrar por nota">
              {[5, 4, 3, 2, 1].map((n) => {
                const cantidad = conteo[n];
                const porcentaje = datos.total ? Math.round((cantidad / datos.total) * 100) : 0;
                return (
                  <li key={n}>
                    <button
                      type="button"
                      className={nota === n ? "cal-barra activa" : "cal-barra"}
                      aria-pressed={nota === n}
                      disabled={!cantidad}
                      onClick={() => setNota(nota === n ? 0 : n)}
                    >
                      <span className="cal-barra-nota">
                        {n} <span aria-hidden="true">★</span>
                        <span className="sr-only"> estrellas</span>
                      </span>
                      <span className="cal-barra-pista" aria-hidden="true">
                        <span style={{ width: `${porcentaje}%` }} />
                      </span>
                      <span className="cal-barra-cant">{cantidad}</span>
                    </button>
                  </li>
                );
              })}
            </ul>

            <p className="cal-nota-pie">
              {conComentario === 1 ? "1 reseña tiene" : `${conComentario} reseñas tienen`} comentario. Los comentarios
              ocultados por el administrador no se muestran, pero su nota sí cuenta en el promedio.
            </p>
          </aside>

          {/* ---------- Reseñas ---------- */}
          <div className="cal-lista-caja">
            <div className="cal-filtros">
              <label className="cal-check">
                <input type="checkbox" checked={soloComentarios} onChange={(e) => setSoloComentarios(e.target.checked)} />
                Solo con comentario
              </label>
              <div className="segmentos" role="group" aria-label="Ordenar reseñas">
                {ORDENES.map((o) => (
                  <button
                    key={o.valor}
                    type="button"
                    className={orden === o.valor ? "activo" : ""}
                    aria-pressed={orden === o.valor}
                    onClick={() => setOrden(o.valor)}
                  >
                    {o.texto}
                  </button>
                ))}
              </div>
            </div>

            {hayFiltro && (
              <p className="cal-filtro-activo" role="status">
                {visibles.length} de {datos.total}
                {nota > 0 && ` · ${nota} ${nota === 1 ? "estrella" : "estrellas"}`}
                {soloComentarios && " · con comentario"}
                <button type="button" onClick={limpiar}>
                  Quitar filtros
                </button>
              </p>
            )}

            {visibles.length === 0 ? (
              <p className="cal-vacio">No hay reseñas con estos filtros.</p>
            ) : (
              <ul className="cal-lista">
                {visibles.map((c) => (
                  <li key={c.id} className={`cal-item nota-${c.estrellas}`}>
                    <div className="cal-item-cab">
                      <Estrellas valor={c.estrellas} tamano={15} />
                      <strong>{c.cliente}</strong>
                      <span className="cal-fecha">{fechaCorta(c.fecha)}</span>
                    </div>
                    {c.comentario ? (
                      <p className="cal-comentario">«{c.comentario}»</p>
                    ) : (
                      <p className="cal-sin-comentario">Sin comentario</p>
                    )}
                    {c.ticket && <span className="cal-ticket">Servicio {c.ticket}</span>}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </section>
  );
}

export default Calificaciones;
