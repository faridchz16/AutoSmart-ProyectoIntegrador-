import EnConstruccion from "../../components/EnConstruccion";

// Aquí irá el selector de fechas a bloquear (T22.2)
function Bloqueos() {
  return (
    <EnConstruccion
      titulo="Fechas bloqueadas"
      texto="Aquí marcarás los días en que no atenderás para que nadie reserve en esas fechas."
    />
  );
}

export default Bloqueos;
