import { useCallback, useEffect, useRef, useState } from "react";
import {
  DIAS,
  listarHorarios,
  eliminarHorario,
  duracionMinutos,
  formatoHoras,
} from "../../services/horariosService";
import Modal from "../../components/admin/Modal";
import Aviso from "../../components/admin/Aviso";
import FormHorario from "./FormHorario";
import "../../components/admin/admin-ui.css";
import "./Horarios.css";

/* T21.2 – "Mis horarios": el mecánico registra por día de la semana
   los rangos en que atiende (usa /mechanics/me/schedule, T21.1). */

const IconoEditar = () => (
  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M4 20h4L18.5 9.5a2.1 2.1 0 0 0-4-4L4 16v4Z" />
    <path d="m13.5 6.5 4 4" />
  </svg>
);

const IconoBorrar = () => (
  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M4 7h16M9 7V4.5h6V7M6.5 7l1 13h9l1-13" />
  </svg>
);

const totalMinutos = (lista) => lista.reduce((t, h) => t + duracionMinutos(h.horaInicio, h.horaFin), 0);

function Horarios() {
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, horarios: [], error: "" });
  const [formulario, setFormulario] = useState(null); // { horario: null | {...}, dia }
  const [porEliminar, setPorEliminar] = useState(null);
  const [eliminando, setEliminando] = useState(false);
  const [aviso, setAviso] = useState(null);

  const primeraCarga = resultado.clave === null;
  const cargando = resultado.clave !== recarga;

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  useEffect(() => {
    let vigente = true;
    listarHorarios()
      .then((horarios) => vigente && setResultado({ clave: recarga, horarios, error: "" }))
      .catch((e) => vigente && setResultado({ clave: recarga, horarios: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recarga]);

  const { horarios, error } = resultado;
  const porDia = DIAS.map((_, i) => horarios.filter((h) => h.diaSemana === i + 1));
  const diasConAtencion = porDia.filter((l) => l.length).length;
  const minutosSemana = totalMinutos(horarios);

  const ordenar = (lista) =>
    [...lista].sort((a, b) => a.diaSemana - b.diaSemana || a.horaInicio.localeCompare(b.horaInicio));

  const alGuardar = (guardado, editando) => {
    setFormulario(null);
    setResultado((r) => ({
      ...r,
      horarios: ordenar(editando ? r.horarios.map((h) => (h.id === guardado.id ? guardado : h)) : [...r.horarios, guardado]),
    }));
    const dia = DIAS[guardado.diaSemana - 1];
    avisar("ok", editando ? `Se actualizó el horario del ${dia}.` : `Se agregó el horario del ${dia}.`);
  };

  const confirmarEliminar = async () => {
    const h = porEliminar;
    setEliminando(true);
    try {
      await eliminarHorario(h.id);
      setResultado((r) => ({ ...r, horarios: r.horarios.filter((x) => x.id !== h.id) }));
      avisar("ok", `Se eliminó el horario del ${DIAS[h.diaSemana - 1]} de ${h.horaInicio} a ${h.horaFin}.`);
      setPorEliminar(null);
    } catch (e) {
      avisar("error", e.message);
    } finally {
      setEliminando(false);
    }
  };

  // Rangos de los demás horarios (para detectar cruces en el formulario)
  const otros = formulario ? horarios.filter((h) => h.id !== formulario.horario?.id) : [];

  return (
    <section className="gestion">
      <div className="gestion-cabecera">
        <div>
          <h2>Mis horarios</h2>
          <p>Registra los días y horas en que atiendes. Los clientes solo podrán reservar dentro de estos rangos.</p>
        </div>
        <button type="button" className="btn btn-pri" onClick={() => setFormulario({ horario: null, dia: 1 })}>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          Agregar horario
        </button>
      </div>

      {!primeraCarga && !error && (
        <div className="hor-resumen" aria-live="polite">
          <span><strong>{diasConAtencion}</strong> {diasConAtencion === 1 ? "día" : "días"} con atención</span>
          <span><strong>{minutosSemana ? formatoHoras(minutosSemana) : "0 h"}</strong> por semana</span>
        </div>
      )}

      {error ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudieron cargar tus horarios</p>
            <p>{error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        </div>
      ) : (
        <div className={cargando && !primeraCarga ? "semana actualizando" : "semana"} aria-busy={cargando}>
          {DIAS.map((dia, i) => {
            const rangos = porDia[i];
            const minutosDia = totalMinutos(rangos);
            return (
              <article key={dia} className={rangos.length ? "dia" : "dia dia-libre"} aria-label={dia}>
                <header className="dia-cab">
                  <h3>{dia}</h3>
                  {!primeraCarga && (
                    <span className="dia-total">{rangos.length ? formatoHoras(minutosDia) : "Libre"}</span>
                  )}
                </header>

                {primeraCarga ? (
                  <span className="esq esq-largo" aria-hidden="true" />
                ) : rangos.length ? (
                  <ul className="rangos">
                    {rangos.map((h) => (
                      <li key={h.id} className="rango">
                        <span className="rango-horas">
                          {h.horaInicio} – {h.horaFin}
                        </span>
                        <span className="rango-acciones">
                          <button
                            type="button"
                            className="btn-mini"
                            onClick={() => setFormulario({ horario: h, dia: h.diaSemana })}
                            aria-label={`Editar ${dia} de ${h.horaInicio} a ${h.horaFin}`}
                            title="Editar"
                          >
                            <IconoEditar />
                          </button>
                          <button
                            type="button"
                            className="btn-mini peligro"
                            onClick={() => setPorEliminar(h)}
                            aria-label={`Eliminar ${dia} de ${h.horaInicio} a ${h.horaFin}`}
                            title="Eliminar"
                          >
                            <IconoBorrar />
                          </button>
                        </span>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="dia-vacio">No atiendes este día.</p>
                )}

                {!primeraCarga && (
                  <button
                    type="button"
                    className="dia-agregar"
                    onClick={() => setFormulario({ horario: null, dia: i + 1 })}
                    aria-label={`Agregar horario el ${dia}`}
                  >
                    + Agregar
                  </button>
                )}
              </article>
            );
          })}
        </div>
      )}

      {/* ---------- Agregar / editar ---------- */}
      <Modal
        abierto={!!formulario}
        alCerrar={() => setFormulario(null)}
        titulo={formulario?.horario ? "Editar horario" : "Agregar horario"}
        pequeno
      >
        {formulario && (
          <FormHorario
            horario={formulario.horario}
            diaInicial={formulario.dia}
            otros={otros}
            alGuardar={alGuardar}
            alCerrar={() => setFormulario(null)}
          />
        )}
      </Modal>

      {/* ---------- Confirmar eliminación ---------- */}
      <Modal abierto={!!porEliminar} alCerrar={() => setPorEliminar(null)} titulo="¿Eliminar horario?" pequeno>
        {porEliminar && (
          <div className="confirmar">
            <p>
              Dejarás de atender el <strong>{DIAS[porEliminar.diaSemana - 1]}</strong> de{" "}
              <strong>{porEliminar.horaInicio}</strong> a <strong>{porEliminar.horaFin}</strong>. Los clientes ya no
              podrán reservar en ese rango.
            </p>
            <div className="form-acciones">
              <button type="button" className="btn btn-sec" onClick={() => setPorEliminar(null)} disabled={eliminando}>
                Cancelar
              </button>
              <button type="button" className="btn btn-peligro" onClick={confirmarEliminar} disabled={eliminando}>
                {eliminando ? "Eliminando..." : "Eliminar"}
              </button>
            </div>
          </div>
        )}
      </Modal>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Horarios;
