import PaginaEstado from "../components/PaginaEstado";

function NoEncontrado() {
  return (
    <PaginaEstado
      codigo="404"
      titulo="No encontramos esta página"
      texto="La dirección no existe o fue movida. Revisa el enlace o vuelve al inicio."
    />
  );
}

export default NoEncontrado;
