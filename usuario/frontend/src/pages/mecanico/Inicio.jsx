import { Link } from "react-router-dom";
import { obtenerUsuario } from "../../services/sesion";
import { ENLACES_MECANICO } from "../../layouts/enlacesMecanico";

/* Inicio del panel del mecánico (TEC2.1): saludo y accesos a cada sección. */
function Inicio() {
  const nombre = obtenerUsuario()?.correo?.split("@")[0] ?? "";
  const secciones = ENLACES_MECANICO.filter((e) => !e.fin);

  return (
    <>
      <div className="adm-intro">
        <h2>Hola, {nombre}</h2>
        <p>Desde aquí organizas tu disponibilidad y las citas que te asignan.</p>
      </div>

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
