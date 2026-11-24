import { useEffect, useState } from "react";
import {
  listarCatalogo,
  listarMecanicos,
  listarTurnos,
  CATEGORIAS_SERVICIO,
  DIAS_RESERVA,
  diaSemana,
  mesCorto,
  hora12,
} from "../../services/reservaService";
import { formatoPrecio, formatoDuracion } from "../../services/serviciosService";
import { hoyLima, sumarDias } from "../../utils/fechas";
import Estrellas from "../../components/Estrellas";

/* Pasos de la reserva de citas (CLI-03): servicio, mecánico y turno. Los usa Reservar.jsx. */

// Carga datos según una clave: { clave, datos, error } y "cargando" mientras la clave no coincida
function useCarga(clave, cargar) {
  const [resultado, setResultado] = useState({ clave: null, datos: [], error: "" });
  useEffect(() => {
    let vigente = true;
    cargar()
      .then((datos) => vigente && setResultado({ clave, datos, error: "" }))
      .catch((e) => vigente && setResultado({ clave, datos: [], error: e.message }));
    return () => {
      vigente = false;
    };
    // cargar cambia en cada render; la clave resume todo lo que se consulta
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clave]);
  return { ...resultado, cargando: resultado.clave !== clave };
}

function EstadoCarga({ error, titulo, alReintentar, children }) {
  return (
    <div className="tabla-caja">
      <div className="tabla-estado">
        {error ? (
          <>
            <p className="tabla-estado-titulo">{titulo}</p>
            <p>{error}</p>
            <button type="button" className="btn btn-sec" onClick={alReintentar}>
              Reintentar
            </button>
          </>
        ) : (
          children
        )}
      </div>
    </div>
  );
}

/* ---------- Paso 1: servicio ---------- */

const sinTildes = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();

export function PasoServicio({ elegido, alElegir }) {
  const [recarga, setRecarga] = useState(0);
  const [busqueda, setBusqueda] = useState("");
  const [categoria, setCategoria] = useState("");
  const { datos, error, cargando, clave } = useCarga(recarga, listarCatalogo);

  if (error || clave === null) {
    return (
      <EstadoCarga error={error} titulo="No se pudo cargar el catálogo" alReintentar={() => setRecarga((n) => n + 1)}>
        <p>Cargando servicios...</p>
      </EstadoCarga>
    );
  }

  const texto = sinTildes(busqueda.trim());
  const lista = datos.filter(
    (s) => (!categoria || s.categoria === categoria) && (!texto || sinTildes(`${s.nombre} ${s.descripcion}`).includes(texto)),
  );

  return (
    <>
      <div className="filtros">
        <label className="buscador">
          <span className="sr-only">Buscar servicio</span>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input type="search" placeholder="Buscar servicio" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
        </label>
        <div className="segmentos" role="group" aria-label="Tipo de servicio">
          {[["", "Todos"], ...Object.entries(CATEGORIAS_SERVICIO)].map(([valor, nombre]) => (
            <button key={valor || "todos"} type="button" className={categoria === valor ? "activo" : ""} aria-pressed={categoria === valor} onClick={() => setCategoria(valor)}>
              {nombre}
            </button>
          ))}
        </div>
      </div>

      {lista.length === 0 ? (
        <EstadoCarga>
          <p className="tabla-estado-titulo">{datos.length ? "Ningún servicio coincide" : "El catálogo está vacío"}</p>
          <p>{datos.length ? "Prueba con otra palabra o muestra todos los tipos." : "El taller aún no publica servicios para reservar."}</p>
        </EstadoCarga>
      ) : (
        <ul className={cargando ? "res-opciones actualizando" : "res-opciones"}>
          {lista.map((s) => (
            <li key={s.id}>
              <button type="button" className={elegido?.id === s.id ? "res-opcion elegida" : "res-opcion"} aria-pressed={elegido?.id === s.id} onClick={() => alElegir(s)}>
                <span className={`res-cat res-cat-${s.categoria}`}>{CATEGORIAS_SERVICIO[s.categoria]}</span>
                <strong className="res-opcion-titulo">{s.nombre}</strong>
                {s.descripcion && <span className="res-opcion-texto">{s.descripcion}</span>}
                <span className="res-opcion-pie">
                  <span>{formatoDuracion(s.duracion)}</span>
                  <strong>{formatoPrecio(s.precio)}</strong>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

/* ---------- Paso 2: mecánico ---------- */

const iniciales = (nombre) =>
  nombre
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join("");

export function PasoMecanico({ servicio, elegido, alElegir, alCambiarServicio }) {
  const [recarga, setRecarga] = useState(0);
  const { datos, error, clave } = useCarga(`${servicio.id}|${recarga}`, () => listarMecanicos(servicio.id));

  if (error || clave === null) {
    return (
      <EstadoCarga error={error} titulo="No se pudieron cargar los mecánicos" alReintentar={() => setRecarga((n) => n + 1)}>
        <p>Buscando mecánicos para {servicio.nombre}...</p>
      </EstadoCarga>
    );
  }
  if (!datos.length) {
    return (
      <EstadoCarga>
        <p className="tabla-estado-titulo">Ningún mecánico atiende este servicio por ahora</p>
        <p>{servicio.especialidad ? `Se necesita la especialidad ${servicio.especialidad}. ` : ""}Elige otro servicio o vuelve a intentarlo más tarde.</p>
        <button type="button" className="btn btn-sec" onClick={alCambiarServicio}>
          Elegir otro servicio
        </button>
      </EstadoCarga>
    );
  }

  return (
    <>
      {servicio.especialidad && (
        <p className="res-nota">
          Te mostramos solo a quienes tienen la especialidad <strong>{servicio.especialidad}</strong>, ordenados por su calificación.
        </p>
      )}
      <ul className="res-opciones">
        {datos.map((m) => (
          <li key={m.id}>
            <button type="button" className={elegido?.id === m.id ? "res-opcion res-mec elegida" : "res-opcion res-mec"} aria-pressed={elegido?.id === m.id} onClick={() => alElegir(m)}>
              <span className="res-mec-cab">
                <span className="res-mec-avatar" aria-hidden="true">
                  {iniciales(m.nombre)}
                </span>
                <span>
                  <strong className="res-opcion-titulo">{m.nombre}</strong>
                  <span className="res-mec-exp">
                    {m.experiencia ? `${m.experiencia} ${m.experiencia === 1 ? "año" : "años"} de experiencia` : "Mecánico del taller"}
                  </span>
                </span>
              </span>
              <span className="res-mec-calif">
                {m.promedio !== null && m.resenas > 0 ? (
                  <>
                    <Estrellas valor={m.promedio} tamano={15} />
                    <strong>{m.promedio.toLocaleString("es-PE", { minimumFractionDigits: 1 })}</strong>
                    <span>
                      ({m.resenas} {m.resenas === 1 ? "reseña" : "reseñas"})
                    </span>
                  </>
                ) : (
                  <span className="res-sin-resenas">Aún sin reseñas</span>
                )}
              </span>
              {m.especialidades.length > 0 && (
                <span className="res-chips">
                  {m.especialidades.map((e) => (
                    <span key={e} className={e === servicio.especialidad ? "res-chip destacada" : "res-chip"}>
                      {e}
                    </span>
                  ))}
                </span>
              )}
            </button>
          </li>
        ))}
      </ul>
    </>
  );
}

/* ---------- Paso 3: fecha y hora ---------- */

export function PasoHorario({ servicio, mecanico, fecha, alCambiarFecha, bloqueo, bloqueando, alElegir, recargaExterna }) {
  const [recarga, setRecarga] = useState(0);
  const hoy = hoyLima();
  const dias = Array.from({ length: DIAS_RESERVA }, (_, i) => sumarDias(hoy, i));
  const { datos, error, cargando, clave } = useCarga(`${servicio.id}|${mecanico.id}|${fecha}|${recarga}|${recargaExterna}`, () =>
    listarTurnos(servicio.id, mecanico.id, fecha),
  );

  // El backend no lista el turno que este cliente ya bloqueó: se vuelve a mostrar como elegido
  const propio = bloqueo && bloqueo.fecha === fecha && bloqueo.idMecanico === mecanico.id ? { inicio: bloqueo.inicio, fin: bloqueo.fin } : null;
  const turnos = propio && !datos.some((t) => t.inicio === propio.inicio) ? [...datos, propio].sort((a, b) => a.inicio.localeCompare(b.inicio)) : datos;
  const grupos = [
    ["Mañana", turnos.filter((t) => t.inicio < "12:00")],
    ["Tarde", turnos.filter((t) => t.inicio >= "12:00")],
  ].filter(([, lista]) => lista.length);
  const indice = dias.indexOf(fecha);

  return (
    <>
      <div className="res-dias" role="group" aria-label="Elige el día">
        {dias.map((d, i) => (
          <button key={d} type="button" className={d === fecha ? "res-dia elegido" : "res-dia"} aria-pressed={d === fecha} onClick={() => alCambiarFecha(d)}>
            <span>{i === 0 ? "Hoy" : i === 1 ? "Mañana" : diaSemana(d).slice(0, 3)}</span>
            <strong>{Number(d.slice(8, 10))}</strong>
            <span>{mesCorto(d)}</span>
          </button>
        ))}
      </div>

      <div className={cargando && clave !== null ? "res-turnos actualizando" : "res-turnos"} aria-busy={cargando} aria-live="polite">
        {error ? (
          <EstadoCarga error={error} titulo="No se pudieron cargar los turnos" alReintentar={() => setRecarga((n) => n + 1)} />
        ) : clave === null || (cargando && !turnos.length) ? (
          <p className="res-turnos-vacio">Buscando turnos libres...</p>
        ) : !turnos.length ? (
          <div className="res-turnos-vacio">
            <p>
              <strong>No hay turnos libres este día.</strong> Puede que el taller no atienda o que {mecanico.nombre.split(" ")[0]} ya tenga la agenda llena.
            </p>
            {indice > -1 && indice < dias.length - 1 && (
              <button type="button" className="btn btn-sec" onClick={() => alCambiarFecha(dias[indice + 1])}>
                Ver el día siguiente
              </button>
            )}
          </div>
        ) : (
          grupos.map(([nombre, lista]) => (
            <div key={nombre} className="res-grupo">
              <h4>{nombre}</h4>
              <div className="res-horas">
                {lista.map((t) => {
                  const esPropio = propio?.inicio === t.inicio;
                  const esperando = bloqueando === t.inicio;
                  return (
                    <button
                      key={t.inicio}
                      type="button"
                      className={esPropio ? "res-hora elegida" : "res-hora"}
                      aria-pressed={esPropio}
                      disabled={!!bloqueando}
                      onClick={() => alElegir(t)}
                    >
                      {esperando && <span className="btn-spinner" aria-hidden="true" />}
                      {hora12(t.inicio)}
                    </button>
                  );
                })}
              </div>
            </div>
          ))
        )}
      </div>
      <p className="res-nota">
        Cada turno dura {formatoDuracion(servicio.duracion)}. Al elegirlo lo separamos para ti por 10 minutos mientras confirmas.
      </p>
    </>
  );
}
