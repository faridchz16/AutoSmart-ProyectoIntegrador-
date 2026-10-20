import { useCallback, useEffect, useRef, useState } from "react";
import {
  obtenerPerfil,
  guardarPerfil,
  listarSugerencias,
  capitalizar,
  mismaEspecialidad,
  MAX_ANIOS,
  MAX_ESPECIALIDADES,
  MAX_LARGO_ESPECIALIDAD,
} from "../../services/perfilMecanicoService";
import Aviso from "../../components/admin/Aviso";
import "../../components/admin/admin-ui.css";
import "./Perfil.css";

/* T13.2 / T14.1 – Perfil profesional del mecánico: años de experiencia y especialidades.
   Los clientes solo pueden reservar con mecánicos que tengan perfil, y el sistema
   los filtra según la especialidad que pide cada servicio (T24.1). */

const iniciales = (nombre) =>
  nombre
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join("") || "?";

const claveDe = ({ aniosExperiencia, especialidades }) =>
  `${aniosExperiencia}|${especialidades.map((e) => e.toLowerCase()).sort().join(",")}`;

const textoAnios = (n) => (n === 1 ? "1 año de experiencia" : `${n} años de experiencia`);

function Perfil() {
  const [recarga, setRecarga] = useState(0);
  const [cargado, setCargado] = useState({ clave: null, perfil: null, error: "" });
  const [anios, setAnios] = useState("0");
  const [especialidades, setEspecialidades] = useState([]);
  const [texto, setTexto] = useState("");
  const [errorEsp, setErrorEsp] = useState("");
  const [sugerencias, setSugerencias] = useState([]);
  const [guardando, setGuardando] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState("");
  const [aviso, setAviso] = useState(null);
  const entradaRef = useRef(null);

  const contadorAvisos = useRef(0);
  const avisar = (tipo, mensaje) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto: mensaje, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  const aplicar = (perfil) => {
    setAnios(String(perfil.aniosExperiencia));
    setEspecialidades(perfil.especialidades);
    setTexto("");
    setErrorEsp("");
  };

  useEffect(() => {
    let vigente = true;
    obtenerPerfil()
      .then((perfil) => {
        if (!vigente) return;
        setCargado({ clave: recarga, perfil, error: "" });
        aplicar(perfil);
      })
      .catch((e) => vigente && setCargado({ clave: recarga, perfil: null, error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recarga]);

  useEffect(() => {
    let vigente = true;
    listarSugerencias().then((s) => vigente && setSugerencias(s));
    return () => {
      vigente = false;
    };
  }, []);

  const { perfil, error } = cargado;
  const cargando = cargado.clave !== recarga;

  // ---------- Validación ----------
  const aniosNum = anios === "" ? NaN : Number(anios);
  const errorAnios =
    anios === "" || !Number.isInteger(aniosNum)
      ? "Ingresa un número entero."
      : aniosNum < 0 || aniosNum > MAX_ANIOS
        ? `Debe estar entre 0 y ${MAX_ANIOS}.`
        : "";

  const lleno = especialidades.length >= MAX_ESPECIALIDADES;
  const hayCambios =
    !!perfil &&
    (texto.trim() !== "" ||
      claveDe({ aniosExperiencia: aniosNum, especialidades }) !== claveDe(perfil));

  // Avisa antes de cerrar o recargar la pestaña con cambios sin guardar
  useEffect(() => {
    if (!hayCambios) return;
    const alSalir = (e) => e.preventDefault();
    window.addEventListener("beforeunload", alSalir);
    return () => window.removeEventListener("beforeunload", alSalir);
  }, [hayCambios]);

  // Devuelve la lista resultante o null si el texto no es válido
  const intentarAgregar = (valor, lista = especialidades) => {
    const nombre = valor.trim().replace(/\s+/g, " ");
    if (!nombre) return lista;
    if (nombre.length > MAX_LARGO_ESPECIALIDAD) {
      setErrorEsp(`Cada especialidad puede tener hasta ${MAX_LARGO_ESPECIALIDAD} caracteres.`);
      return null;
    }
    const repetida = lista.find((e) => mismaEspecialidad(e, nombre));
    if (repetida) {
      setErrorEsp(`«${repetida}» ya está en tu lista.`);
      return null;
    }
    if (lista.length >= MAX_ESPECIALIDADES) {
      setErrorEsp(`Puedes registrar hasta ${MAX_ESPECIALIDADES} especialidades.`);
      return null;
    }
    setErrorEsp("");
    return [...lista, capitalizar(nombre)];
  };

  const agregar = (valor) => {
    const nueva = intentarAgregar(valor);
    if (nueva) {
      setEspecialidades(nueva);
      setTexto("");
    }
  };

  const quitar = (nombre) => {
    setEspecialidades((lista) => lista.filter((e) => e !== nombre));
    setErrorEsp("");
    entradaRef.current?.focus();
  };

  const alTeclear = (e) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      agregar(texto);
    } else if (e.key === "Backspace" && texto === "" && especialidades.length) {
      setEspecialidades((lista) => lista.slice(0, -1));
    }
  };

  const cambiarAnios = (paso) => {
    const base = Number.isInteger(aniosNum) ? aniosNum : 0;
    setAnios(String(Math.min(MAX_ANIOS, Math.max(0, base + paso))));
  };

  const descartar = () => {
    aplicar(perfil);
    setErrorGeneral("");
  };

  const guardar = async (e) => {
    e.preventDefault();
    if (errorAnios || guardando) return;
    // Si quedó texto escrito sin pulsar Enter, también se agrega
    const lista = intentarAgregar(texto);
    if (!lista) return;
    setGuardando(true);
    setErrorGeneral("");
    try {
      const guardado = await guardarPerfil({ aniosExperiencia: aniosNum, especialidades: lista });
      setCargado((c) => ({ ...c, perfil: guardado }));
      aplicar(guardado);
      avisar("ok", "Tu perfil se guardó correctamente.");
    } catch (err) {
      setErrorGeneral(err.message);
    } finally {
      setGuardando(false);
    }
  };

  const disponibles = sugerencias.filter((s) => !especialidades.some((e) => mismaEspecialidad(e, s)));
  const incompleto = perfil && perfil.especialidades.length === 0;

  // La vista previa muestra lo que se está editando (sin guardar todavía)
  const previa = {
    nombre: perfil?.nombre || "Tu nombre",
    correo: perfil?.correo ?? "",
    anios: errorAnios ? null : aniosNum,
    especialidades,
  };

  return (
    <section className="gestion">
      <div className="gestion-cabecera">
        <div>
          <h2>Mi perfil profesional</h2>
          <p>Tus especialidades y experiencia ayudan a los clientes a elegirte al reservar.</p>
        </div>
      </div>

      {error ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudo cargar tu perfil</p>
            <p>{error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        </div>
      ) : cargando && !perfil ? (
        <div className="pf" aria-busy="true">
          <div className="pf-tarjeta"><span className="esq esq-largo" /></div>
          <div className="pf-form"><span className="esq esq-largo" /></div>
        </div>
      ) : (
        <>
          {incompleto && (
            <p className="aviso-fijo">
              Aún no registras especialidades. Agrega al menos una para aparecer en los servicios que la requieren.
            </p>
          )}

          <div className="pf">
            {/* ---------- Vista previa ---------- */}
            <aside className="pf-tarjeta" aria-label="Vista previa de tu perfil">
              <p className="pf-etiqueta">Así te verán los clientes</p>
              <div className="pf-cabeza">
                <span className="pf-avatar" aria-hidden="true">{iniciales(previa.nombre)}</span>
                <div>
                  <h3>{previa.nombre}</h3>
                  <span className="pf-correo">{previa.correo}</span>
                </div>
              </div>
              <p className="pf-anios">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="m12 3 2.6 5.6 6.1.7-4.5 4.2 1.2 6L12 16.6 6.6 19.5l1.2-6-4.5-4.2 6.1-.7z" />
                </svg>
                {previa.anios === null ? "—" : previa.anios === 0 ? "Sin experiencia registrada" : textoAnios(previa.anios)}
              </p>
              {previa.especialidades.length ? (
                <ul className="pf-chips">
                  {previa.especialidades.map((e) => (
                    <li key={e} className="pf-chip">{e}</li>
                  ))}
                </ul>
              ) : (
                <p className="pf-sin">Sin especialidades</p>
              )}
              {hayCambios && <p className="pf-pendiente">Vista previa con cambios sin guardar</p>}
            </aside>

            {/* ---------- Formulario ---------- */}
            <form className="pf-form adm-form" onSubmit={guardar} noValidate>
              {errorGeneral && <p className="form-error-general" role="alert">{errorGeneral}</p>}

              <div className="campo">
                <label htmlFor="pf-anios">Años de experiencia</label>
                <div className="pf-stepper">
                  <button type="button" onClick={() => cambiarAnios(-1)} disabled={aniosNum <= 0} aria-label="Restar un año">
                    −
                  </button>
                  <input
                    id="pf-anios"
                    type="number"
                    inputMode="numeric"
                    min={0}
                    max={MAX_ANIOS}
                    step={1}
                    value={anios}
                    onChange={(e) => setAnios(e.target.value)}
                    aria-invalid={!!errorAnios}
                    aria-describedby="pf-anios-ayuda"
                  />
                  <button type="button" onClick={() => cambiarAnios(1)} disabled={aniosNum >= MAX_ANIOS} aria-label="Sumar un año">
                    +
                  </button>
                </div>
                {errorAnios ? (
                  <p className="campo-error" id="pf-anios-ayuda">{errorAnios}</p>
                ) : (
                  <p className="campo-ayuda" id="pf-anios-ayuda">Entre 0 y {MAX_ANIOS} años.</p>
                )}
              </div>

              <div className="campo">
                <div className="campo-fila">
                  <label htmlFor="pf-esp">Especialidades</label>
                  <span className={lleno ? "contador excedido" : "contador"}>
                    {especialidades.length}/{MAX_ESPECIALIDADES}
                  </span>
                </div>
                <div
                  className={errorEsp ? "pf-caja invalida" : "pf-caja"}
                  onClick={() => entradaRef.current?.focus()}
                >
                  {especialidades.map((e) => (
                    <span key={e} className="pf-chip editable">
                      {e}
                      <button
                        type="button"
                        onClick={(ev) => {
                          ev.stopPropagation();
                          quitar(e);
                        }}
                        aria-label={`Quitar ${e}`}
                      >
                        ×
                      </button>
                    </span>
                  ))}
                  <input
                    id="pf-esp"
                    ref={entradaRef}
                    type="text"
                    value={texto}
                    maxLength={MAX_LARGO_ESPECIALIDAD + 1}
                    onChange={(e) => {
                      setTexto(e.target.value);
                      if (errorEsp) setErrorEsp("");
                    }}
                    onKeyDown={alTeclear}
                    placeholder={lleno ? "Llegaste al máximo" : especialidades.length ? "Agregar otra…" : "Ej. Frenos"}
                    disabled={lleno}
                    aria-invalid={!!errorEsp}
                    aria-describedby="pf-esp-ayuda"
                  />
                </div>
                {errorEsp ? (
                  <p className="campo-error" id="pf-esp-ayuda" role="alert">{errorEsp}</p>
                ) : (
                  <p className="campo-ayuda" id="pf-esp-ayuda">Escribe y pulsa Enter para agregar. Haz clic en × para quitar.</p>
                )}
              </div>

              {disponibles.length > 0 && (
                <div className="pf-sugerencias">
                  <p>Sugerencias</p>
                  <div>
                    {disponibles.map((s) => (
                      <button key={s} type="button" className="pf-sug" onClick={() => agregar(s)} disabled={lleno}>
                        + {s}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div className="form-acciones">
                <button type="button" className="btn btn-sec" onClick={descartar} disabled={!hayCambios || guardando}>
                  Descartar cambios
                </button>
                <button type="submit" className="btn btn-pri" disabled={!hayCambios || !!errorAnios || guardando}>
                  {guardando && <span className="btn-spinner" aria-hidden="true" />}
                  {guardando ? "Guardando..." : "Guardar perfil"}
                </button>
              </div>
            </form>
          </div>
        </>
      )}

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Perfil;
