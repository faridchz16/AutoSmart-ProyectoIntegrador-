import { useNavigate } from "react-router-dom";
import PaginaEstado from "../components/PaginaEstado";
import { cerrarSesion } from "../services/sesion";

function NoAutorizado() {
  const navigate = useNavigate();

  const salir = () => {
    cerrarSesion();
    navigate("/login", { replace: true });
  };

  return (
    <PaginaEstado
      codigo="403"
      titulo="No tienes acceso a esta sección"
      texto="Tu cuenta no tiene los permisos necesarios. Si crees que es un error, inicia sesión con otra cuenta."
    >
      <button type="button" className="estado-btn sec" onClick={salir}>
        Cambiar de cuenta
      </button>
    </PaginaEstado>
  );
}

export default NoAutorizado;
