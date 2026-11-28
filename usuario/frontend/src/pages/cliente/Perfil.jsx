import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { obtenerPerfil, guardarPerfil, MAX_NOMBRE, PATRON_TELEFONO } from "../../services/perfilClienteService";
import Aviso from "../../components/admin/Aviso";
import "../../components/admin/admin-ui.css";
import "./Cliente.css";

/* CLI-01 – Mi perfil del cliente (API de T05.1: GET y PUT /users/me).
   Nombre y teléfono editables; el correo es la cuenta y no se cambia. El teléfono es opcional
   (7 a 15 dígitos, puede empezar con +): el taller lo usa para avisar cuando el vehículo está listo. */

const iniciales = (nombre) =>
  String(nombre ?? "")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join("") || "?";

function validar({ nombre, telefono }) {
  const e = {};
  if (!nombre.trim()) e.nombre = "Escribe tu nombre.";
  else if (nombre.trim().length > MAX_NOMBRE) e.nombre = `Máximo ${MAX_NOMBRE} caracteres.`;
  const tel = telefono.replace(/[\s-]/g, "");
  if (tel && !PATRON_TELEFONO.test(tel)) e.telefono = "Escribe entre 7 y 15 dígitos (puede empezar con +), por ejemplo 987654321.";
  return e;
}

function Perfil() {
  const [recarga, setRecarga] = useState(0);
  const [cargado, setCargado] = useState({ clave: null, perfil: null, error: "" });
  const [datos, setDatos] = useState({ nombre: "", telefono: "" });
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [aviso, setAviso] = useState(null);

  const contadorAvisos = useRef(0);
  const cerrarAviso = useCallback(() => setAviso(null), []);

  useEffect(() => {
    let vigente = true;
    obtenerPerfil()
      .then((perfil) => {
        if (!vigente) return;
        setCargado({ clave: recarga, perfil, error: "" });
        setDatos({ nombre: perfil.nombre, telefono: perfil.telefono });
      })
      .catch((e) => vigente && setCargado({ clave: recarga, perfil: null, error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recarga]);

  const { perfil } = cargado;
  const cambios = !!perfil && (datos.nombre.trim() !== perfil.nombre || datos.telefono.replace(/[\s-]/g, "") !== perfil.telefono);

  const cambiar = (campo) => (e) => {
    setDatos((d) => ({ ...d, [campo]: e.target.value }));
    setErrores((x) => ({ ...x, [campo]: undefined }));
    setErrorGeneral("");
  };

  const enviar = async (ev) => {
    ev.preventDefault();
    const e = validar(datos);
    setErrores(e);
    if (Object.keys(e).length) return;
    setGuardando(true);
    setErrorGeneral("");
    try {
      const actualizado = await guardarPerfil(datos);
      setCargado((c) => ({ ...c, perfil: actualizado }));
      setDatos({ nombre: actualizado.nombre, telefono: actualizado.telefono });
      contadorAvisos.current += 1;
      setAviso({ tipo: "ok", texto: "Se guardaron los datos de tu perfil.", id: contadorAvisos.current });
    } catch (err) {
      setErrorGeneral(err.message);
    } finally {
      setGuardando(false);
    }
  };

  if (cargado.error) {
    return (
      <section className="gestion">
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudo cargar tu perfil</p>
            <p>{cargado.error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="gestion">
      <div className="gestion-cabecera">
        <div>
          <h2>Mi perfil</h2>
          <p>Tus datos de contacto para las citas y los avisos del taller.</p>
        </div>
      </div>

      <div className="cli-perfil">
        <aside className="cli-tarjeta" aria-label="Tu cuenta">
          <span className="cli-avatar" aria-hidden="true">
            {iniciales(datos.nombre || perfil?.nombre)}
          </span>
          <div>
            <h3>{(datos.nombre.trim() || perfil?.nombre) ?? "Cargando..."}</h3>
            <p>{perfil?.correo}</p>
          </div>
          <dl>
            <dt>Tipo de cuenta</dt>
            <dd>Cliente</dd>
            <dt>Teléfono</dt>
            <dd>{perfil?.telefono || <span className="cli-vacio">Sin registrar</span>}</dd>
          </dl>
          <p className="cli-nota">
            ¿Quieres cambiar tu contraseña? Usa <Link to="/recuperar">Recuperar contraseña</Link> y te enviaremos un código a tu correo.
          </p>
        </aside>

        <form className="adm-form cli-form" onSubmit={enviar} noValidate aria-busy={!perfil}>
          <div className="campo">
            <label htmlFor="cli-nombre">Nombre completo</label>
            <input
              id="cli-nombre"
              autoComplete="name"
              maxLength={MAX_NOMBRE}
              value={datos.nombre}
              disabled={!perfil}
              aria-invalid={!!errores.nombre}
              onChange={cambiar("nombre")}
            />
            {errores.nombre && <p className="campo-error">{errores.nombre}</p>}
          </div>
          <div className="campo">
            <label htmlFor="cli-correo">Correo electrónico</label>
            <input id="cli-correo" type="email" value={perfil?.correo ?? ""} disabled readOnly />
            <span className="campo-ayuda">Es tu usuario para iniciar sesión; no se puede cambiar.</span>
          </div>
          <div className="campo">
            <label htmlFor="cli-telefono">
              Teléfono <span className="opcional">(opcional)</span>
            </label>
            <input
              id="cli-telefono"
              type="tel"
              inputMode="tel"
              autoComplete="tel"
              placeholder="987654321"
              value={datos.telefono}
              disabled={!perfil}
              aria-invalid={!!errores.telefono}
              onChange={cambiar("telefono")}
            />
            {errores.telefono ? (
              <p className="campo-error">{errores.telefono}</p>
            ) : (
              <span className="campo-ayuda">El taller lo usa para avisarte cuando tu vehículo esté listo.</span>
            )}
          </div>

          {errorGeneral && (
            <p className="form-error-general" role="alert">
              {errorGeneral}
            </p>
          )}
          <div className="form-acciones">
            <button
              type="button"
              className="btn btn-sec"
              disabled={!cambios || guardando}
              onClick={() => {
                setDatos({ nombre: perfil.nombre, telefono: perfil.telefono });
                setErrores({});
                setErrorGeneral("");
              }}
            >
              Descartar
            </button>
            <button type="submit" className="btn btn-pri" disabled={!cambios || guardando}>
              {guardando && <span className="btn-spinner" aria-hidden="true" />}
              {guardando ? "Guardando..." : "Guardar cambios"}
            </button>
          </div>
        </form>
      </div>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Perfil;
