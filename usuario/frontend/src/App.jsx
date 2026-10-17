import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Recuperar from "./pages/Recuperar";
import RestablecerPassword from "./pages/RestablecerPassword";
import NoAutorizado from "./pages/NoAutorizado";
import NoEncontrado from "./pages/NoEncontrado";
import RutaProtegida from "./components/RutaProtegida";
import AdminLayout from "./layouts/AdminLayout";
import Resumen from "./pages/admin/Resumen";
import Usuarios from "./pages/admin/Usuarios";
import Servicios from "./pages/admin/Servicios";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Páginas públicas */}
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/registro" element={<Register />} />
        <Route path="/recuperar" element={<Recuperar />} />
        <Route path="/recuperar/nueva" element={<RestablecerPassword />} />
        <Route path="/sin-acceso" element={<NoAutorizado />} />

        {/* Panel de administración: solo con sesión iniciada y rol ADMIN */}
        <Route element={<RutaProtegida roles={["ADMIN"]} />}>
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<Resumen />} />
            <Route path="usuarios" element={<Usuarios />} />
            <Route path="servicios" element={<Servicios />} />
          </Route>
        </Route>

        {/* Cualquier otra dirección */}
        <Route path="*" element={<NoEncontrado />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
