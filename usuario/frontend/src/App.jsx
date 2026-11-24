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
import MecanicoLayout from "./layouts/MecanicoLayout";
import Resumen from "./pages/admin/Resumen";
import Usuarios from "./pages/admin/Usuarios";
import Servicios from "./pages/admin/Servicios";
import Reservas from "./pages/admin/Reservas";
import Taller from "./pages/admin/Taller";
import Inventario from "./pages/admin/Inventario";
import Solicitudes from "./pages/admin/Solicitudes";
import Indicadores from "./pages/admin/Indicadores";
import Pagos from "./pages/admin/Pagos";
import HistorialPlaca from "./pages/admin/HistorialPlaca";
import CalificacionesAdmin from "./pages/admin/Calificaciones";
import Derivaciones from "./pages/admin/Derivaciones";
import PreguntasChatbot from "./pages/admin/PreguntasChatbot";
import InicioMecanico from "./pages/mecanico/Inicio";
import Agenda from "./pages/mecanico/Agenda";
import CitasHoy from "./pages/mecanico/CitasHoy";
import Horarios from "./pages/mecanico/Horarios";
import Bloqueos from "./pages/mecanico/Bloqueos";
import Perfil from "./pages/mecanico/Perfil";
import OrdenTrabajo from "./pages/mecanico/OrdenTrabajo";
import Calificaciones from "./pages/mecanico/Calificaciones";
import ClienteLayout from "./layouts/ClienteLayout";
import InicioCliente from "./pages/cliente/Inicio";
import PerfilCliente from "./pages/cliente/Perfil";
import VehiculosCliente from "./pages/cliente/Vehiculos";
import ReservarCita from "./pages/cliente/Reservar";
import Historial from "./pages/mecanico/Historial";

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
            <Route path="indicadores" element={<Indicadores />} />
            <Route path="reservas" element={<Reservas />} />
            <Route path="pagos" element={<Pagos />} />
            <Route path="historial-placa" element={<HistorialPlaca />} />
            <Route path="calificaciones" element={<CalificacionesAdmin />} />
            <Route path="derivaciones" element={<Derivaciones />} />
            <Route path="preguntas-chatbot" element={<PreguntasChatbot />} />
            <Route path="usuarios" element={<Usuarios />} />
            <Route path="servicios" element={<Servicios />} />
            <Route path="inventario" element={<Inventario />} />
            <Route path="solicitudes" element={<Solicitudes />} />
            <Route path="taller" element={<Taller />} />
          </Route>
        </Route>

        {/* Panel del mecánico: solo con sesión iniciada y rol MECANICO (TEC2.1) */}
        <Route element={<RutaProtegida roles={["MECANICO"]} />}>
          <Route path="/mecanico" element={<MecanicoLayout />}>
            <Route index element={<InicioMecanico />} />
            <Route path="agenda" element={<Agenda />} />
            <Route path="citas-hoy" element={<CitasHoy />} />
            <Route path="horarios" element={<Horarios />} />
            <Route path="bloqueos" element={<Bloqueos />} />
            <Route path="perfil" element={<Perfil />} />
            <Route path="historial" element={<Historial />} />
            <Route path="calificaciones" element={<Calificaciones />} />
            <Route path="citas/:idCita/orden" element={<OrdenTrabajo />} />
          </Route>
        </Route>

        {/* Panel del cliente: solo con sesión iniciada y rol CLIENTE (Sprint 4) */}
        <Route element={<RutaProtegida roles={["CLIENTE"]} />}>
          <Route path="/cliente" element={<ClienteLayout />}>
            <Route index element={<InicioCliente />} />
            <Route path="reservar" element={<ReservarCita />} />
            <Route path="vehiculos" element={<VehiculosCliente />} />
            <Route path="perfil" element={<PerfilCliente />} />
          </Route>
        </Route>

        {/* Cualquier otra dirección */}
        <Route path="*" element={<NoEncontrado />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
