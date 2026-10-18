import EnConstruccion from "../../components/EnConstruccion";

// Aquí irán las citas del día ordenadas por hora (T34.1), su detalle (T35.2) y aceptar/rechazar (T36.2)
function CitasHoy() {
  return (
    <EnConstruccion
      titulo="Citas de hoy"
      texto="Aquí verás las citas del día ordenadas por hora, con su detalle y las opciones para aceptarlas o rechazarlas."
    />
  );
}

export default CitasHoy;
