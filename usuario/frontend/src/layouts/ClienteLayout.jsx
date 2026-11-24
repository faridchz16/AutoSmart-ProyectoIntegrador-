import PanelLayout from "./PanelLayout";
import ChatBot from "../components/ChatBot";
import { ENLACES_CLIENTE, TITULOS_CLIENTE } from "./enlacesCliente";

/* Panel web del cliente: la misma estructura que los paneles del administrador y del mecánico.
   Incluye el asistente (chatbot), que con la sesión iniciada también consulta el estado de su vehículo
   y agenda citas conversando. */

function ClienteLayout() {
  return (
    <>
      <PanelLayout enlaces={ENLACES_CLIENTE} titulos={TITULOS_CLIENTE} seccion="Mi cuenta" inicio="/cliente" avisoPrueba />
      <ChatBot />
    </>
  );
}

export default ClienteLayout;
