import { useCallback, useEffect, useRef, useState } from "react";
import {
  listarServicios,
  cambiarEstadoServicio,
  formatoPrecio,
  formatoDuracion,
} from "../../services/serviciosService";
import Modal from "../../components/admin/Modal";
import Switch from "../../components/admin/Switch";
import Paginacion from "../../components/admin/Paginacion";
import Aviso from "../../components/admin/Aviso";
import FormServicio from "./FormServicio";
import "../../components/admin/admin-ui.css";

/* Catálogo de servicios:
   - T18.2 Formulario "Nuevo servicio" con validaciones
   - T19.2 Edición desde la tabla
   - T20.1 Switch activo / inactivo (deshabilitar sin borrar) */

const TAMANO = 8;

const FILTROS = [
  { valor: "", texto: "Todos" },
  { valor: "activos", texto: "Activos" },
  { valor: "inactivos", texto: "Inactivos" },
];

const sinTildes = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();

const IconoEditar = () => (
  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M4 20h4L18.5 9.5a2.1 2.1 0 0 0-4-4L4 16v4Z" />
    <path d="m13.5 6.5 4 4" />
  </svg>
);

function Servicios() {
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, servicios: [], soloActivos: false, error: "" });
  const [estado, setEstado] = useState("");
  const [busqueda, setBusqueda] = useState("");
  const [pagina, setPagina] = useState(0);
  const [formulario, setFormulario] = useState(null); // { servicio: null | {...} }
  const [porDesactivar, setPorDesactivar] = useState(null);
  const [cambiando, setCambiando] = useState([]);
  const [aviso, setAviso] = useState(null);

  const cargando = resultado.clave !== recarga;
  const primeraCarga = resultado.clave === null;

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  useEffect(() => {
    let vigente = true;
    listarServicios()
      .then((r) => vigente && setResultado({ clave: recarga, ...r, error: "" }))
      .catch((e) => vigente && setResultado({ clave: recarga, servicios: [], soloActivos: false, error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recarga]);

  // Filtro y búsqueda en el navegador (el catálogo es pequeño)
  const texto = sinTildes(busqueda.trim());
  const filtrados = resultado.servicios
    .filter((s) => (estado === "activos" ? s.activo : estado === "inactivos" ? !s.activo : true))
    .filter((s) => !texto || sinTildes(`${s.nombre} ${s.descripcion}`).includes(texto))
    .sort((a, b) => a.nombre.localeCompare(b.nombre, "es"));
  const paginas = Math.max(1, Math.ceil(filtrados.length / TAMANO));
  const paginaActual = Math.min(pagina, paginas - 1);
  const visibles = filtrados.slice(paginaActual * TAMANO, paginaActual * TAMANO + TAMANO);
  const activos = resultado.servicios.filter((s) => s.activo).length;

  const reemplazar = (id, cambios) =>
    setResultado((r) => ({ ...r, servicios: r.servicios.map((s) => (s.id === id ? { ...s, ...cambios } : s)) }));

  const aplicarEstado = async (servicio, activo) => {
    setCambiando((c) => [...c, servicio.id]);
    try {
      const r = await cambiarEstadoServicio(servicio.id, activo);
      reemplazar(servicio.id, { activo: r.activo });
      avisar("ok", r.activo ? `"${servicio.nombre}" vuelve a estar en el catálogo.` : `"${servicio.nombre}" ya no se muestra en el catálogo.`);
    } catch (e) {
      avisar("error", e.message);
    } finally {
      setCambiando((c) => c.filter((id) => id !== servicio.id));
    }
  };

  const alCambiarEstado = (servicio, activo) => {
    if (activo) aplicarEstado(servicio, true);
    else setPorDesactivar(servicio);
  };

  const alGuardar = (guardado, editando) => {
    setFormulario(null);
    if (editando) {
      reemplazar(guardado.id, guardado);
      avisar("ok", `Se guardaron los cambios de "${guardado.nombre}".`);
    } else {
      setResultado((r) => ({ ...r, servicios: [...r.servicios, guardado] }));
      setEstado("");
      setBusqueda(guardado.nombre);
      setPagina(0);
      avisar("ok", `Se creó el servicio "${guardado.nombre}".`);
    }
  };

  return (
    <section className="gestion">
      <div className="gestion-cabecera">
        <div>
          <h2>Servicios</h2>
          <p>
            Administra el catálogo que ven los clientes.
            {!primeraCarga && !resultado.error && (
              <> <strong>{activos}</strong> de <strong>{resultado.servicios.length}</strong> activos.</>
            )}
          </p>
        </div>
        <button type="button" className="btn btn-pri" onClick={() => setFormulario({ servicio: null })}>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          Nuevo servicio
        </button>
      </div>

      {resultado.soloActivos && (
        <p className="aviso-fijo" role="note">
          El servidor solo devuelve los servicios <strong>activos</strong>. Si desactivas uno, dejará de verse aquí al
          recargar la página (falta el endpoint <code>GET /admin/services</code> en el backend).
        </p>
      )}

      {/* ---------- Filtros ---------- */}
      <div className="filtros">
        <label className="buscador">
          <span className="sr-only">Buscar servicio</span>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input
            type="search"
            placeholder="Buscar servicio"
            value={busqueda}
            onChange={(e) => {
              setBusqueda(e.target.value);
              setPagina(0);
            }}
          />
        </label>

        <div className="segmentos" role="group" aria-label="Filtrar por estado">
          {FILTROS.map((f) => (
            <button
              key={f.valor || "todos"}
              type="button"
              className={estado === f.valor ? "activo" : ""}
              aria-pressed={estado === f.valor}
              onClick={() => {
                setEstado(f.valor);
                setPagina(0);
              }}
            >
              {f.texto}
            </button>
          ))}
        </div>
      </div>

      {/* ---------- Tabla ---------- */}
      <div className={cargando && !primeraCarga ? "tabla-caja actualizando" : "tabla-caja"} aria-busy={cargando}>
        {resultado.error ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudo cargar el catálogo</p>
            <p>{resultado.error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        ) : !primeraCarga && filtrados.length === 0 ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">
              {resultado.servicios.length ? "Sin resultados" : "Aún no hay servicios"}
            </p>
            <p>
              {resultado.servicios.length
                ? "No hay servicios que coincidan con la búsqueda."
                : "Crea el primer servicio del catálogo con el botón «Nuevo servicio»."}
            </p>
          </div>
        ) : (
          <table className="tabla">
            <thead>
              <tr>
                <th scope="col">Servicio</th>
                <th scope="col" className="col-num">Precio base</th>
                <th scope="col">Duración</th>
                <th scope="col">Estado</th>
                <th scope="col"><span className="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {primeraCarga
                ? Array.from({ length: 5 }, (_, i) => (
                    <tr key={i} className="fila-esqueleto" aria-hidden="true">
                      <td><span className="esq esq-largo" /></td>
                      <td><span className="esq" /></td>
                      <td><span className="esq" /></td>
                      <td><span className="esq" /></td>
                      <td />
                    </tr>
                  ))
                : visibles.map((s) => (
                    <tr key={s.id} className={s.activo ? "" : "fila-inactiva"}>
                      <td data-label="Servicio">
                        <div className="servicio-info">
                          <strong>{s.nombre}</strong>
                          {s.descripcion && <span>{s.descripcion}</span>}
                        </div>
                      </td>
                      <td data-label="Precio base" className="col-num">
                        <span className="precio">{formatoPrecio(s.precioBase)}</span>
                      </td>
                      <td data-label="Duración">{formatoDuracion(s.duracionMinutos)}</td>
                      <td data-label="Estado">
                        <Switch
                          activo={s.activo}
                          cargando={cambiando.includes(s.id)}
                          etiqueta={`Servicio ${s.nombre}`}
                          alCambiar={(activo) => alCambiarEstado(s, activo)}
                        />
                      </td>
                      <td data-label="Acciones" className="col-acciones">
                        <button
                          type="button"
                          className="btn-icono"
                          onClick={() => setFormulario({ servicio: s })}
                          aria-label={`Editar ${s.nombre}`}
                        >
                          <IconoEditar />
                          <span>Editar</span>
                        </button>
                      </td>
                    </tr>
                  ))}
            </tbody>
          </table>
        )}
      </div>

      {!resultado.error && (
        <Paginacion
          pagina={paginaActual}
          paginas={paginas}
          total={filtrados.length}
          tamano={TAMANO}
          alCambiar={setPagina}
        />
      )}

      {/* ---------- Crear / editar ---------- */}
      <Modal
        abierto={!!formulario}
        alCerrar={() => setFormulario(null)}
        titulo={formulario?.servicio ? "Editar servicio" : "Nuevo servicio"}
      >
        {formulario && (
          <FormServicio servicio={formulario.servicio} alGuardar={alGuardar} alCerrar={() => setFormulario(null)} />
        )}
      </Modal>

      {/* ---------- Confirmar desactivación ---------- */}
      <Modal abierto={!!porDesactivar} alCerrar={() => setPorDesactivar(null)} titulo="¿Desactivar servicio?" pequeno>
        {porDesactivar && (
          <div className="confirmar">
            <p>
              <strong>{porDesactivar.nombre}</strong> dejará de mostrarse en el catálogo para los clientes.
              No se borra: su historial se conserva y puedes activarlo cuando quieras.
            </p>
            <div className="form-acciones">
              <button type="button" className="btn btn-sec" onClick={() => setPorDesactivar(null)}>
                Cancelar
              </button>
              <button
                type="button"
                className="btn btn-peligro"
                onClick={() => {
                  aplicarEstado(porDesactivar, false);
                  setPorDesactivar(null);
                }}
              >
                Desactivar
              </button>
            </div>
          </div>
        )}
      </Modal>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Servicios;
