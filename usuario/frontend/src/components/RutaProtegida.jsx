import { Navigate, Outlet, useLocation } from "react-router-dom";
import { sesionActiva, obtenerUsuario, normalizarRol } from "../services/sesion";

/* Protege pantallas que requieren sesión iniciada.
   - Sin sesión            -> /login (y después vuelve a la pantalla que quería)
   - Con sesión, otro rol  -> /sin-acceso
   Uso:
     <Route element={<RutaProtegida roles={["ADMIN"]} />}> ...rutas... </Route>
     <Route element={<RutaProtegida />}> ...rutas para cualquier usuario... </Route> */
function RutaProtegida({ roles, children }) {
  const ubicacion = useLocation();

  if (!sesionActiva()) {
    return <Navigate to="/login" replace state={{ from: ubicacion }} />;
  }

  if (roles && !roles.map(normalizarRol).includes(normalizarRol(obtenerUsuario()?.rol))) {
    return <Navigate to="/sin-acceso" replace />;
  }

  return children ?? <Outlet />;
}

export default RutaProtegida;
