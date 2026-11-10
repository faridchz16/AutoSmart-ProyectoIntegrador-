import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { obtenerUsuario, expiracionSesion, normalizarRol } from "../../services/sesion";
import { listarMateriales, enAlerta, textoUnidad } from "../../services/inventarioService";

const Flecha = () => <span aria-hidden="true">→</span>;

/* T60.1 – Contador de alertas de stock mínimo en el panel */
function AlertasStock() {
  const [estado, setEstado] = useState({ cargando: true, alertas: [], error: false });

  useEffect(() => {
    let vigente = true;
    listarMateriales()
      .then((m) => vigente && setEstado({ cargando: false, alertas: m.filter(enAlerta), error: false }))
      .catch(() => vigente && setEstado({ cargando: false, alertas: [], error: true }));
    return () => {
      vigente = false;
    };
  }, []);

  if (estado.error) return null;
  const n = estado.alertas.length;
  const agotados = estado.alertas.filter((m) => m.stockActual === 0).length;
  const primeros = [...estado.alertas].sort((a, b) => a.stockActual - b.stockActual).slice(0, 3);

  return (
    <Link
      to={n ? "/admin/inventario?filtro=bajo-stock" : "/admin/inventario"}
      className={n ? "adm-card adm-alertas con-alertas" : "adm-card adm-alertas"}
    >
      <div className="adm-alertas-cab">
        <span className="adm-alertas-num">{estado.cargando ? "…" : n}</span>
        <h3>{n === 1 ? "Material por reponer" : "Materiales por reponer"}</h3>
      </div>
      <p>
        {estado.cargando
          ? "Revisando el almacén..."
          : n
            ? `Llegaron a su stock mínimo${agotados ? ` · ${agotados} ${agotados === 1 ? "agotado" : "agotados"}` : ""}.`
            : "Todo el almacén está por encima del stock mínimo."}
      </p>
      {primeros.length > 0 && (
        <ul className="adm-alertas-lista">
          {primeros.map((m) => (
            <li key={m.id}>
              <span>{m.nombre}</span>
              <strong>
                {m.stockActual} / mín. {m.stockMinimo} {textoUnidad(m.unidad, 2)}
              </strong>
            </li>
          ))}
        </ul>
      )}
      <span className="adm-mas">
        {n ? "Ver materiales por reponer" : "Abrir inventario"} <Flecha />
      </span>
    </Link>
  );
}

function Resumen() {
  const usuario = obtenerUsuario();
  const nombre = usuario?.correo?.split("@")[0] ?? "";
  const exp = expiracionSesion();

  return (
    <>
      <div className="adm-intro">
        <h2>Hola, {nombre}</h2>
        <p>Desde aquí administras las reservas, las cuentas, los servicios, el inventario y el horario de AutoSmart.</p>
      </div>

      <div className="adm-cards">
        <AlertasStock />

        <Link to="/admin/reservas" className="adm-card">
          <span className="adm-card-icono">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <rect x="3.5" y="5" width="17" height="15" rx="2" />
              <path d="M3.5 9.5h17M8 3v4M16 3v4" />
              <path d="m9 14.5 2 2 4-4" />
            </svg>
          </span>
          <h3>Reservas</h3>
          <p>Revisa las citas del taller por fecha, estado y mecánico.</p>
          <span className="adm-mas">Abrir reservas <Flecha /></span>
        </Link>

        <Link to="/admin/usuarios" className="adm-card">
          <span className="adm-card-icono">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <circle cx="9" cy="8" r="3.2" />
              <path d="M3 20c.6-3.3 3-5 6-5s5.4 1.7 6 5" />
              <circle cx="17" cy="9" r="2.6" />
              <path d="M16.5 14.2c2.6.2 4.1 1.8 4.5 4.8" />
            </svg>
          </span>
          <h3>Usuarios</h3>
          <p>Consulta las cuentas registradas, crea mecánicos y activa o desactiva accesos.</p>
          <span className="adm-mas">Abrir usuarios <Flecha /></span>
        </Link>

        <Link to="/admin/servicios" className="adm-card">
          <span className="adm-card-icono">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M14.7 6.3a4 4 0 0 0-5.4 5.1L3.5 17.2a1.8 1.8 0 0 0 2.6 2.6l5.8-5.8a4 4 0 0 0 5.1-5.4l-2.5 2.5-2.3-.5-.5-2.3 2.5-2.5Z" />
            </svg>
          </span>
          <h3>Servicios</h3>
          <p>Registra los tipos de servicio, edita sus tarifas y habilítalos o deshabilítalos.</p>
          <span className="adm-mas">Abrir servicios <Flecha /></span>
        </Link>

        <Link to="/admin/taller" className="adm-card">
          <span className="adm-card-icono">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="8.5" />
              <path d="M12 7.5V12l3 2" />
            </svg>
          </span>
          <h3>Horario del taller</h3>
          <p>Define qué días y en qué horario atiende el taller, y registra los feriados.</p>
          <span className="adm-mas">Abrir horario <Flecha /></span>
        </Link>

        <section className="adm-card adm-sesion" aria-label="Tu sesión">
          <h3>Tu sesión</h3>
          <dl>
            <dt>Correo</dt>
            <dd>{usuario?.correo}</dd>
            <dt>Rol</dt>
            <dd>{normalizarRol(usuario?.rol)}</dd>
            {exp && (
              <>
                <dt>Vence</dt>
                <dd>{new Date(exp).toLocaleString("es-PE", { dateStyle: "medium", timeStyle: "short" })}</dd>
              </>
            )}
          </dl>
        </section>
      </div>
    </>
  );
}

export default Resumen;
