import { useCallback, useEffect, useRef, useState } from "react";
import { listarUsuarios, cambiarEstadoUsuario, NOMBRE_ROL } from "../../services/usuariosService";
import { obtenerUsuario } from "../../services/sesion";
import Modal from "../../components/admin/Modal";
import Switch from "../../components/admin/Switch";
import Paginacion from "../../components/admin/Paginacion";
import Aviso from "../../components/admin/Aviso";
import NuevoMecanico from "./NuevoMecanico";
import "../../components/admin/admin-ui.css";

/* Gestión de usuarios:
   - T16.2 Tabla con filtro por rol, búsqueda y paginación
   - T17.2 Switch para activar o desactivar cuentas
   - T15.2 Botón y formulario "Nuevo mecánico" */

const TAMANO = 8;

const FILTROS = [
  { valor: "", texto: "Todos" },
  { valor: "CLIENTE", texto: "Clientes" },
  { valor: "MECANICO", texto: "Mecánicos" },
  { valor: "ADMIN", texto: "Administradores" },
];

const iniciales = (nombre) =>
  nombre.split(" ").filter(Boolean).slice(0, 2).map((p) => p[0]).join("").toUpperCase() || "?";

const fecha = (iso) =>
  iso ? new Date(iso).toLocaleDateString("es-PE", { day: "2-digit", month: "short", year: "numeric" }) : "—";

function Usuarios() {
  const [rol, setRol] = useState("");
  const [busqueda, setBusqueda] = useState(""); // lo que se escribe
  const [q, setQ] = useState("");               // lo que se busca (con retraso)
  const [pagina, setPagina] = useState(0);
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, usuarios: [], total: 0, paginas: 1, error: "" });
  const [cambiando, setCambiando] = useState([]); // ids con el switch en proceso
  const [porDesactivar, setPorDesactivar] = useState(null);
  const [nuevoAbierto, setNuevoAbierto] = useState(false);
  const [aviso, setAviso] = useState(null);

  const miCorreo = obtenerUsuario()?.correo;
  const clave = `${rol}|${q}|${pagina}|${recarga}`;
  const cargando = resultado.clave !== clave;
  const primeraCarga = resultado.clave === null;

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  // La búsqueda se aplica cuando el usuario deja de escribir
  useEffect(() => {
    const t = setTimeout(() => {
      setQ(busqueda.trim());
      setPagina(0);
    }, 350);
    return () => clearTimeout(t);
  }, [busqueda]);

  // Carga la página de usuarios cada vez que cambia un filtro
  useEffect(() => {
    let vigente = true;
    listarUsuarios({ rol, q, pagina, tamano: TAMANO })
      .then((r) => vigente && setResultado({ clave, ...r, error: "" }))
      .catch((e) => vigente && setResultado({ clave, usuarios: [], total: 0, paginas: 1, error: e.message }));
    return () => {
      vigente = false;
    };
  }, [clave, rol, q, pagina]);

  const elegirRol = (valor) => {
    setRol(valor);
    setPagina(0);
  };

  const aplicarEstado = async (usuario, activo) => {
    setCambiando((c) => [...c, usuario.id]);
    try {
      const r = await cambiarEstadoUsuario(usuario.id, activo);
      setResultado((res) => ({
        ...res,
        usuarios: res.usuarios.map((u) => (u.id === usuario.id ? { ...u, activo: r.activo } : u)),
      }));
      avisar("ok", `${usuario.nombre} ahora está ${r.activo ? "activo" : "inactivo"}.`);
    } catch (e) {
      avisar("error", e.message);
    } finally {
      setCambiando((c) => c.filter((id) => id !== usuario.id));
    }
  };

  // Desactivar pide confirmación; activar es directo
  const alCambiarEstado = (usuario, activo) => {
    if (activo) aplicarEstado(usuario, true);
    else setPorDesactivar(usuario);
  };

  const alCrearMecanico = (usuario) => {
    setRol("");
    setBusqueda("");
    setQ("");
    setPagina(0);
    setRecarga((n) => n + 1);
    avisar("ok", `Se creó la cuenta de ${usuario.nombre}.`);
  };

  const { usuarios, total, paginas, error } = resultado;

  return (
    <section className="gestion">
      <div className="gestion-cabecera">
        <div>
          <h2>Usuarios</h2>
          <p>Supervisa las cuentas registradas y controla quién puede ingresar.</p>
        </div>
        <button type="button" className="btn btn-pri" onClick={() => setNuevoAbierto(true)}>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          Nuevo mecánico
        </button>
      </div>

      {/* ---------- Filtros ---------- */}
      <div className="filtros">
        <label className="buscador">
          <span className="sr-only">Buscar por nombre o correo</span>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input
            type="search"
            placeholder="Buscar por nombre o correo"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </label>

        <div className="segmentos" role="group" aria-label="Filtrar por rol">
          {FILTROS.map((f) => (
            <button
              key={f.valor || "todos"}
              type="button"
              className={rol === f.valor ? "activo" : ""}
              aria-pressed={rol === f.valor}
              onClick={() => elegirRol(f.valor)}
            >
              {f.texto}
            </button>
          ))}
        </div>
      </div>

      {/* ---------- Tabla ---------- */}
      <div className={cargando && !primeraCarga ? "tabla-caja actualizando" : "tabla-caja"} aria-busy={cargando}>
        {error ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudo cargar la lista</p>
            <p>{error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        ) : !primeraCarga && !cargando && usuarios.length === 0 ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">Sin resultados</p>
            <p>No hay usuarios que coincidan con los filtros.</p>
          </div>
        ) : (
          <table className="tabla">
            <thead>
              <tr>
                <th scope="col">Usuario</th>
                <th scope="col">Rol</th>
                <th scope="col">Registro</th>
                <th scope="col">Estado</th>
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
                    </tr>
                  ))
                : usuarios.map((u) => {
                    const soyYo = u.correo === miCorreo;
                    return (
                      <tr key={u.id} className={u.activo ? "" : "fila-inactiva"}>
                        <td data-label="Usuario">
                          <div className="persona">
                            <span className={`persona-avatar rol-${u.rol.toLowerCase()}`} aria-hidden="true">
                              {iniciales(u.nombre)}
                            </span>
                            <div>
                              <strong>
                                {u.nombre}
                                {soyYo && <span className="tu"> (tú)</span>}
                              </strong>
                              <span>{u.correo}</span>
                            </div>
                          </div>
                        </td>
                        <td data-label="Rol">
                          <span className={`insignia rol-${u.rol.toLowerCase()}`}>{NOMBRE_ROL[u.rol] ?? u.rol}</span>
                        </td>
                        <td data-label="Registro">{fecha(u.creadoEn)}</td>
                        <td data-label="Estado">
                          <Switch
                            activo={u.activo}
                            cargando={cambiando.includes(u.id)}
                            deshabilitado={soyYo}
                            titulo={soyYo ? "No puedes desactivar tu propia cuenta" : undefined}
                            etiqueta={`Cuenta de ${u.nombre}`}
                            alCambiar={(activo) => alCambiarEstado(u, activo)}
                          />
                        </td>
                      </tr>
                    );
                  })}
            </tbody>
          </table>
        )}
      </div>

      {!error && (
        <Paginacion pagina={pagina} paginas={paginas} total={total} tamano={TAMANO} alCambiar={setPagina} />
      )}

      {/* ---------- Confirmar desactivación ---------- */}
      <Modal abierto={!!porDesactivar} alCerrar={() => setPorDesactivar(null)} titulo="¿Desactivar cuenta?" pequeno>
        {porDesactivar && (
          <div className="confirmar">
            <p>
              <strong>{porDesactivar.nombre}</strong> no podrá iniciar sesión hasta que vuelvas a activar
              su cuenta. Sus datos no se borran.
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

      {/* ---------- Nuevo mecánico ---------- */}
      <Modal abierto={nuevoAbierto} alCerrar={() => setNuevoAbierto(false)} titulo="Nuevo mecánico">
        <NuevoMecanico alCrear={alCrearMecanico} alCerrar={() => setNuevoAbierto(false)} />
      </Modal>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Usuarios;
