import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { obtenerPerfil, primerNombre } from "../../services/perfilClienteService";
import { obtenerUsuario } from "../../services/sesion";
import { ENLACES_CLIENTE } from "../../layouts/enlacesCliente";
import "./Cliente.css";

/* Inicio del panel del cliente: saludo, aviso si falta el teléfono y accesos a cada sección. */
function Inicio() {
  const [perfil, setPerfil] = useState(null);
  const secciones = ENLACES_CLIENTE.filter((e) => !e.fin);

  useEffect(() => {
    let vigente = true;
    obtenerPerfil()
      .then((p) => vigente && setPerfil(p))
      .catch(() => vigente && setPerfil(null));
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
