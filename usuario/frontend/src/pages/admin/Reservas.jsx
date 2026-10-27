import { useCallback, useEffect, useRef, useState } from "react";
import { listarReservas, listarMecanicos, esReasignable } from "../../services/reservasService";
import { ESTADOS_CITA } from "../../services/agendaService";
import { hoyLima, isoDe, aFecha } from "../../services/bloqueosService";
import { formatoPrecio } from "../../services/serviciosService";
import Paginacion from "../../components/admin/Paginacion";
import Aviso from "../../components/admin/Aviso";
import ReasignarReserva from "./ReasignarReserva";
import "../../components/admin/admin-ui.css";
import "./Reservas.css";

/* T38.2 – Tabla de reservas del taller para el administrador:
   filtros por periodo, estado y mecánico, búsqueda por ticket, cliente o placa, y paginación.
   Usa GET /admin/appointments (T38.1).
   T39.2 – Botón "Reasignar" en las citas activas de hoy en adelante (ver ReasignarReserva.jsx). */

const TAMANO = 10;

const PERIODOS = [
  { valor: "hoy", texto: "Hoy" },
  { valor: "semana", texto: "Esta semana" },
  { valor: "mes", texto: "Este mes" },
  { valor: "rango", texto: "Rango" },
];

function sumarDias(iso, dias) {
  const d = aFecha(iso);
  d.setDate(d.getDate() + dias);
  return isoDe(d.getFullYear(), d.getMonth(), d.getDate());
}

// Fechas desde/hasta de cada periodo (semana de lunes a domingo)
function rangoDe(periodo, hoy) {
  const d = aFecha(hoy);
  if (periodo === "hoy") return { desde: hoy, hasta: hoy };
  if (periodo === "semana") {
    const lunes = sumarDias(hoy, -((d.getDay() + 6) % 7));
    return { desde: lunes, hasta: sumarDias(lunes, 6) };
  }
  if (periodo === "mes") {
    const ultimo = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();
    return { desde: isoDe(d.getFullYear(), d.getMonth(), 1), hasta: isoDe(d.getFullYear(), d.getMonth(), ultimo) };
  }
  return null;
}

const corta = (iso) => aFecha(iso).toLocaleDateString("es-PE", { weekday: "short", day: "numeric", month: "short" });
const textoRango = ({ desde, hasta }) =>
  desde === hasta ? corta(desde) : `${corta(desde)} – ${corta(hasta)}`;

function Reservas() {
  const [hoy] = useState(hoyLima);
  const [periodo, setPeriodo] = useState("semana");
  const [rango, setRango] = useState(() => rangoDe("semana", hoyLima()));
  const [estado, setEstado] = useState("");
  const [idMecanico, setIdMecanico] = useState("");
  const [busqueda, setBusqueda] = useState("");
  const [q, setQ] = useState("");
  const [pagina, setPagina] = useState(0);
  const [recarga, setRecarga] = useState(0);
  const [mecanicos, setMecanicos] = useState([]);
  const [resultado, setResultado] = useState({ clave: null, reservas: [], total: 0, paginas: 1, error: "", pendiente: false });
  const [porReasignar, setPorReasignar] = useState(null);
  const [aviso, setAviso] = useState(null);
  const contadorAvisos = useRef(0);
  const cerrarAviso = useCallback(() => setAviso(null), []);

  const rangoValido = !!rango.desde && !!rango.hasta && rango.desde <= rango.hasta;
  const clave = `${rango.desde}|${rango.hasta}|${estado}|${idMecanico}|${q}|${pagina}|${recarga}`;
  const cargando = rangoValido && resultado.clave !== clave;
  const primeraCarga = resultado.clave === null;

  // Mecánicos para el filtro (si falla, el filtro simplemente no se muestra)
  useEffect(() => {
    let vigente = true;
    listarMecanicos()
      .then((lista) => vigente && setMecanicos(lista))
      .catch(() => {});
    return () => {
      vigente = false;
    };
  }, []);

  // La búsqueda se aplica cuando el usuario deja de escribir
  useEffect(() => {
    const t = setTimeout(() => {
      setQ(busqueda.trim());
      setPagina(0);
    }, 350);
    return () => clearTimeout(t);
  }, [busqueda]);

  useEffect(() => {
    if (!rangoValido) return;
    let vigente = true;
    listarReservas({ desde: rango.desde, hasta: rango.hasta, estado, idMecanico, q, pagina, tamano: TAMANO })
      .then((r) => vigente && setResultado({ clave, ...r, error: "", pendiente: false }))
      .catch(
        (e) =>
          vigente &&
          setResultado({ clave, reservas: [], total: 0, paginas: 1, error: e.message, pendiente: !!e.pendiente })
      );
    return () => {
      vigente = false;
    };
  }, [clave, rangoValido, rango.desde, rango.hasta, estado, idMecanico, q, pagina]);

  const elegirPeriodo = (valor) => {
    setPeriodo(valor);
    setPagina(0);
    const nuevo = rangoDe(valor, hoy);
    if (nuevo) setRango(nuevo);
  };

  const cambiarRango = (campo) => (e) => {
    setRango((r) => ({ ...r, [campo]: e.target.value }));
    setPagina(0);
  };

  const filtrosActivos = periodo !== "semana" || estado || idMecanico || busqueda;
  const limpiar = () => {
    elegirPeriodo("semana");
    setEstado("");
    setIdMecanico("");
    setBusqueda("");
    setQ("");
  };

  // Tras reasignar: se actualiza la fila al instante y luego se recarga (por si sale del filtro de mecánico)
  const alReasignar = (actualizada) => {
    setPorReasignar(null);
    setResultado((r) => ({
      ...r,
      reservas: r.reservas.map((x) => (x.id === actualizada.id ? { ...x, ...actualizada } : x)),
    }));
    setRecarga((n) => n + 1);
    contadorAvisos.current += 1;
    setAviso({
      tipo: "ok",
      texto: `La reserva ${actualizada.ticket} ahora es de ${actualizada.mecanico}. Queda pendiente hasta que la acepte.`,
      id: contadorAvisos.current,
    });
  };

  const { reservas, total, paginas, error, pendiente } = resultado;
  const conAcciones = reservas.some((r) => esReasignable(r, hoy));
  const paginaActual = Math.min(pagina, paginas - 1);

  return (
    <section className="gestion">
      <div className="gestion-cabecera">
        <div>
          <h2>Reservas</h2>
          <p>
            Todas las citas del taller.
            {rangoValido && (
              <>
                {" "}
                <span className="rs-rango">{textoRango(rango)}</span>
                {!primeraCarga && !error && (
                  <>
                    {" · "}
                    <strong>{total}</strong> {total === 1 ? "reserva" : "reservas"}
                  </>
                )}
              </>
            )}
          </p>
        </div>
      </div>

      {/* ---------- Filtros ---------- */}
      <div className="filtros">
        <div className="segmentos" role="group" aria-label="Periodo">
          {PERIODOS.map((p) => (
            <button
              key={p.valor}
              type="button"
              className={periodo === p.valor ? "activo" : ""}
              aria-pressed={periodo === p.valor}
              onClick={() => elegirPeriodo(p.valor)}
            >
              {p.texto}
            </button>
          ))}
        </div>

        {periodo === "rango" && (
          <div className="rs-fechas">
            <label>
              <span>Desde</span>
              <input type="date" value={rango.desde} max={rango.hasta || undefined} onChange={cambiarRango("desde")} />
            </label>
            <label>
              <span>Hasta</span>
              <input type="date" value={rango.hasta} min={rango.desde || undefined} onChange={cambiarRango("hasta")} />
            </label>
          </div>
        )}
      </div>

      <div className="filtros">
        <label className="buscador">
          <span className="sr-only">Buscar por ticket, cliente o placa</span>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input
            type="search"
            placeholder="Buscar por ticket, cliente o placa"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </label>

        <label className="rs-select">
          <span className="sr-only">Filtrar por estado</span>
          <select
            value={estado}
            onChange={(e) => {
              setEstado(e.target.value);
              setPagina(0);
            }}
          >
            <option value="">Todos los estados</option>
            {Object.entries(ESTADOS_CITA).map(([valor, texto]) => (
              <option key={valor} value={valor}>{texto}</option>
            ))}
          </select>
        </label>

        {mecanicos.length > 0 && (
          <label className="rs-select">
            <span className="sr-only">Filtrar por mecánico</span>
            <select
              value={idMecanico}
              onChange={(e) => {
                setIdMecanico(e.target.value);
                setPagina(0);
              }}
            >
              <option value="">Todos los mecánicos</option>
              {mecanicos.map((m) => (
                <option key={m.id} value={m.id}>{m.nombre}</option>
              ))}
            </select>
          </label>
        )}

        {filtrosActivos && (
          <button type="button" className="rs-limpiar" onClick={limpiar}>
            Limpiar filtros
          </button>
        )}
      </div>

      {!rangoValido && (
        <p className="aviso-fijo" role="alert">
          Elige una fecha de inicio y una de fin; la de inicio no puede ser posterior a la de fin.
        </p>
      )}

      {/* ---------- Tabla ---------- */}
      <div className={cargando && !primeraCarga ? "tabla-caja actualizando" : "tabla-caja"} aria-busy={cargando}>
        {error ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">
              {pendiente ? "Esta sección espera al backend" : "No se pudieron cargar las reservas"}
            </p>
            <p>{error}</p>
            {pendiente ? (
              <p className="rs-ayuda">
                Mientras tanto puedes verla con datos de prueba: agrega <code>VITE_DATOS_PRUEBA=true</code> al archivo{" "}
                <code>.env</code>.
              </p>
            ) : (
              <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
                Reintentar
              </button>
            )}
          </div>
        ) : !primeraCarga && reservas.length === 0 ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">Sin reservas</p>
            <p>
              {estado || idMecanico || q
                ? "No hay reservas que coincidan con los filtros elegidos."
                : "No hay reservas en este periodo."}
            </p>
          </div>
        ) : (
          <table className="tabla rs-tabla">
            <thead>
              <tr>
                <th scope="col">Reserva</th>
                <th scope="col">Cliente</th>
                <th scope="col">Servicio</th>
                <th scope="col">Mecánico</th>
                <th scope="col" className="col-num">Precio</th>
                <th scope="col">Estado</th>
                <th scope="col"><span className="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {primeraCarga
                ? Array.from({ length: 6 }, (_, i) => (
                    <tr key={i} className="fila-esqueleto" aria-hidden="true">
                      <td><span className="esq" /></td>
                      <td><span className="esq esq-largo" /></td>
                      <td><span className="esq esq-largo" /></td>
                      <td><span className="esq" /></td>
                      <td><span className="esq" /></td>
                      <td><span className="esq" /></td>
                      <td />
                    </tr>
                  ))
                : reservas.map((r) => (
                    <tr key={r.id} className={r.estado === "CANCELADA" || r.estado === "RECHAZADA" ? "fila-inactiva" : ""}>
                      <td data-label="Reserva">
                        <div className="rs-cuando">
                          <strong className={r.fecha === hoy ? "rs-hoy" : undefined}>
                            {r.fecha === hoy ? "Hoy" : corta(r.fecha)} · {r.horaInicio}
                          </strong>
                          <span className="rs-ticket">{r.ticket}</span>
                        </div>
                      </td>
                      <td data-label="Cliente">
                        <div className="rs-cliente">
                          <span>{r.cliente}</span>
                          {r.placa && <span className="rs-placa">{r.placa}</span>}
                        </div>
                      </td>
                      <td data-label="Servicio">{r.servicio}</td>
                      <td data-label="Mecánico">{r.mecanico || "—"}</td>
                      <td data-label="Precio" className="col-num">
                        <span className="precio">{r.precio === null ? "—" : formatoPrecio(r.precio)}</span>
                      </td>
                      <td data-label="Estado">
                        <span className={`insignia rs-est-${r.estado}`}>{ESTADOS_CITA[r.estado] ?? r.estado}</span>
                      </td>
                      <td data-label="Acciones" className={conAcciones ? "col-acciones" : "col-acciones rs-sin-acciones"}>
                        {esReasignable(r, hoy) && (
                          <button
                            type="button"
                            className="btn-icono"
                            onClick={() => setPorReasignar(r)}
                            aria-label={`Reasignar ${r.ticket}`}
                          >
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                              <path d="M4 7h13l-3-3M20 17H7l3 3" />
                            </svg>
                            <span>Reasignar</span>
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
            </tbody>
          </table>
        )}
      </div>

      {!error && rangoValido && (
        <Paginacion pagina={paginaActual} paginas={paginas} total={total} tamano={TAMANO} alCambiar={setPagina} />
      )}

      <ReasignarReserva
        reserva={porReasignar}
        alCerrar={() => setPorReasignar(null)}
        alReasignar={alReasignar}
        alDesactualizar={() => setRecarga((n) => n + 1)}
      />
      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Reservas;
