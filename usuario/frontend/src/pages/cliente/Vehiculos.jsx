import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { listarVehiculos, eliminarVehiculo, formatoKm } from "../../services/vehiculosService";
import FormVehiculo from "./FormVehiculo";
import Modal from "../../components/admin/Modal";
import Aviso from "../../components/admin/Aviso";
import "../../components/admin/admin-ui.css";
import "./Cliente.css";
import "./Vehiculos.css";

/* CLI-02 – Mis vehículos del cliente (API de T07.1 a T09.1: /vehicles).
   - Tarjeta por vehículo con su placa, marca, modelo, año y kilometraje
   - Registrar y editar en una ventana con las mismas validaciones del backend
   - Eliminar con confirmación (el taller conserva su historial; la placa queda registrada)
   - "Reservar cita" abre la reserva con ese vehículo ya elegido (CLI-03)
   - "Historial" muestra sus mantenimientos (CLI-08) */

function Placa({ placa }) {
  return (
    <span className="veh-placa">
      <span className="veh-placa-pais" aria-hidden="true">
        PERÚ
      </span>
      <span className="sr-only">Placa </span>
      {placa}
    </span>
  );
}

function Vehiculos() {
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, lista: [], error: "" });
  const [formulario, setFormulario] = useState(null); // { vehiculo } (vehiculo null = nuevo)
  const [porEliminar, setPorEliminar] = useState(null);
  const [eliminando, setEliminando] = useState(false);
  const [errorEliminar, setErrorEliminar] = useState("");
  const [aviso, setAviso] = useState(null);

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  const primeraCarga = resultado.clave === null;
  const cargando = resultado.clave !== recarga;

  useEffect(() => {
    let vigente = true;
    listarVehiculos()
      .then((lista) => vigente && setResultado({ clave: recarga, lista, error: "" }))
      .catch((e) => vigente && setResultado({ clave: recarga, lista: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recarga]);

  const recargar = () => setRecarga((n) => n + 1);

  const alGuardar = (guardado) => {
    const editando = !!formulario?.vehiculo;
    setFormulario(null);
    if (!guardado) {
      // 404: el vehículo ya no existe (p. ej., se eliminó desde la app)
      avisar("error", "Ese vehículo ya no está en tu lista. Actualizamos tus vehículos.");
      return recargar();
    }
    setResultado((r) => ({
      ...r,
      lista: (editando ? r.lista.map((v) => (v.id === guardado.id ? guardado : v)) : [...r.lista, guardado]).sort((a, b) =>
        a.placa.localeCompare(b.placa),
      ),
    }));
    avisar("ok", editando ? `Se actualizaron los datos de ${guardado.placa}.` : `Registraste tu ${guardado.marca} ${guardado.modelo} (${guardado.placa}).`);
  };

  const abrirEliminar = (v) => {
    setPorEliminar(v);
    setErrorEliminar("");
  };

  const confirmarEliminar = async () => {
    setEliminando(true);
    setErrorEliminar("");
    try {
      await eliminarVehiculo(porEliminar.id);
      setResultado((r) => ({ ...r, lista: r.lista.filter((v) => v.id !== porEliminar.id) }));
      avisar("ok", `Quitaste ${porEliminar.placa} de tus vehículos.`);
      setPorEliminar(null);
    } catch (err) {
      if (err.status === 404) {
        setPorEliminar(null);
        avisar("error", "Ese vehículo ya no estaba en tu lista.");
        recargar();
      } else setErrorEliminar(err.message);
    } finally {
      setEliminando(false);
    }
  };

  const { lista } = resultado;
  const nuevo = () => setFormulario({ vehiculo: null });

  return (
    <section className="gestion veh">
      <div className="gestion-cabecera">
        <div>
          <h2>Mis vehículos</h2>
          <p>
            Registra tus autos para reservar citas y llevar su historial de mantenimiento.
            {!primeraCarga && !resultado.error && lista.length > 0 && (
              <>
                {" "}
                Tienes <strong>{lista.length}</strong> {lista.length === 1 ? "vehículo" : "vehículos"}.
              </>
            )}
          </p>
        </div>
        {!resultado.error && lista.length > 0 && (
          <button type="button" className="btn btn-pri" onClick={nuevo}>
            <span aria-hidden="true">+</span> Agregar vehículo
          </button>
        )}
      </div>

      {resultado.error ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudieron cargar tus vehículos</p>
            <p>{resultado.error}</p>
            <button type="button" className="btn btn-sec" onClick={recargar}>
              Reintentar
            </button>
          </div>
        </div>
      ) : primeraCarga ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p>Cargando tus vehículos...</p>
          </div>
        </div>
      ) : lista.length === 0 ? (
        <div className="veh-vacio">
          <span className="veh-vacio-icono" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
              <path d="M5 16V11.5l1.8-4.2A2 2 0 0 1 8.6 6h6.8a2 2 0 0 1 1.8 1.3L19 11.5V16" />
              <path d="M4 16h16v2.5a.5.5 0 0 1-.5.5H18a1 1 0 0 1-1-1v-.5H7v.5a1 1 0 0 1-1 1H4.5a.5.5 0 0 1-.5-.5Z" />
              <path d="M5 11.5h14" />
              <circle cx="8" cy="14" r=".6" fill="currentColor" />
              <circle cx="16" cy="14" r=".6" fill="currentColor" />
            </svg>
          </span>
          <h3>Aún no registras vehículos</h3>
          <p>Agrega tu auto con su placa para poder reservar una cita en el taller.</p>
          <button type="button" className="btn btn-pri" onClick={nuevo}>
            Registrar mi primer vehículo
          </button>
        </div>
      ) : (
        <ul className={cargando ? "veh-lista actualizando" : "veh-lista"} aria-busy={cargando}>
          {lista.map((v) => (
            <li key={v.id} className="veh-item">
              <Placa placa={v.placa} />
              <div className="veh-datos">
                <h3>
                  {v.marca} {v.modelo}
                </h3>
                <dl>
                  <div>
                    <dt>Año</dt>
                    <dd>{v.anio ?? "—"}</dd>
                  </div>
                  <div>
                    <dt>Kilometraje</dt>
                    <dd>{formatoKm(v.kilometraje)}</dd>
                  </div>
                </dl>
              </div>
              <div className="veh-acciones">
                <Link to={`/cliente/reservar?vehiculo=${v.id}`} className="btn btn-pri veh-reservar">
                  Reservar cita
                </Link>
                <Link to={`/cliente/vehiculos/${v.id}/historial`} className="btn btn-sec" aria-label={`Historial de ${v.placa}`}>
                  Historial
                </Link>
                <button type="button" className="btn btn-sec" onClick={() => setFormulario({ vehiculo: v })} aria-label={`Editar ${v.placa}`}>
                  Editar
                </button>
                <button type="button" className="btn btn-sec veh-quitar" onClick={() => abrirEliminar(v)} aria-label={`Eliminar ${v.placa}`}>
                  Eliminar
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <Modal
        abierto={!!formulario}
        alCerrar={() => setFormulario(null)}
        titulo={formulario?.vehiculo ? `Editar ${formulario.vehiculo.placa}` : "Agregar vehículo"}
      >
        {formulario && <FormVehiculo vehiculo={formulario.vehiculo} alGuardar={alGuardar} alCancelar={() => setFormulario(null)} />}
      </Modal>

      <Modal abierto={!!porEliminar} alCerrar={() => !eliminando && setPorEliminar(null)} titulo="Eliminar vehículo" pequeno>
        {porEliminar && (
          <div className="confirmar">
            <p>
              ¿Quitar <strong>{porEliminar.marca} {porEliminar.modelo} ({porEliminar.placa})</strong> de tus vehículos? Ya no podrás
              reservar citas con él. El taller conserva su historial de mantenimiento y la placa queda registrada: si luego quieres volver a
              agregarlo, comunícate con el taller.
            </p>
            {errorEliminar && (
              <p className="form-error-general" role="alert">
                {errorEliminar}
              </p>
            )}
            <div className="form-acciones">
              <button type="button" className="btn btn-sec" onClick={() => setPorEliminar(null)} disabled={eliminando}>
                Cancelar
              </button>
              <button type="button" className="btn btn-peligro" onClick={confirmarEliminar} disabled={eliminando}>
                {eliminando && <span className="btn-spinner" aria-hidden="true" />}
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

export default Vehiculos;
