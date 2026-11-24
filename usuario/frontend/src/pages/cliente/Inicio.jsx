import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { obtenerPerfil, primerNombre } from "../../services/perfilClienteService";
import { obtenerUsuario } from "../../services/sesion";
import { ENLACES_CLIENTE } from "../../layouts/enlacesCliente";
import { proximaCita, ESTADOS_CITA_CLIENTE } from "../../services/citasClienteService";
import { fechaLarga, hora12 } from "../../services/reservaService";
import "./Cliente.css";

/* Inicio del panel del cliente: saludo, aviso si falta el teléfono, su próxima cita y accesos a cada sección. */
function Inicio() {
  const [perfil, setPerfil] = useState(null);
  const [proxima, setProxima] = useState({ cargada: false, cita: null });
  const secciones = ENLACES_CLIENTE.filter((e) => !e.fin);

  useEffect(() => {
    let vigente = true;
    obtenerPerfil()
      .then((p) => vigente && setPerfil(p))
      .catch(() => vigente && setPerfil(null));
    proximaCita()
      .then((cita) => vigente && setProxima({ cargada: true, cita }))
      .catch(() => vigente && setProxima({ cargada: true, cita: null }));
    return () => {
      vigente = false;
    };
  }, []);

  const nombre = primerNombre(perfil?.nombre) || obtenerUsuario()?.correo?.split("@")[0] || "";

  return (
    <>
      <div className="adm-intro">
        <h2>Hola, {nombre}</h2>
        <p>Desde aquí gestionas tus vehículos, tus citas y el mantenimiento de tu auto.</p>
      </div>

      {perfil && !perfil.telefono && (
        <Link to="/cliente/perfil" className="cli-aviso">
          <strong>Agrega tu teléfono</strong>
          <span>Así el taller podrá avisarte cuando tu vehículo esté listo. Completar tu perfil →</span>
        </Link>
      )}

      {proxima.cargada &&
        (proxima.cita ? (
          <Link to={`/cliente/citas?nueva=${proxima.cita.id}`} className="cli-proxima">
            <span className="cli-proxima-etiqueta">Tu próxima cita · {ESTADOS_CITA_CLIENTE[proxima.cita.estado].texto}</span>
            <strong>
              {proxima.cita.servicio} · {fechaLarga(proxima.cita.fecha)}, {hora12(proxima.cita.inicio)}
            </strong>
            <span>
              {proxima.cita.placa} con {proxima.cita.mecanico} · Ver mis citas →
            </span>
          </Link>
        ) : (
          <Link to="/cliente/reservar" className="cli-proxima vacia">
            <span className="cli-proxima-etiqueta">Sin citas próximas</span>
            <strong>¿Le toca mantenimiento a tu auto?</strong>
            <span>Reserva una cita en cuatro pasos →</span>
          </Link>
        ))}

      <div className="adm-cards">
        {secciones.map((s) => (
          <Link key={s.to} to={s.to} className="adm-card">
            <span className="adm-card-icono">{s.icono}</span>
            <h3>{s.texto}</h3>
            <p>{s.descripcion}</p>
            <span className="adm-mas">
              Abrir <span aria-hidden="true">→</span>
            </span>
          </Link>
        ))}
      </div>
    </>
  );
}

export default Inicio;
