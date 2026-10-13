import { Link } from "react-router-dom";
import "./PaginaEstado.css";

/* Pantalla simple para avisos: "Sin acceso" (403) y "No encontrada" (404). */
function PaginaEstado({ codigo, titulo, texto, children }) {
  return (
    <main className="estado">
      <div className="estado-caja">
        <img src="/logo.png" alt="" width="56" height="56" />
        <span className="estado-codigo">{codigo}</span>
        <h1>{titulo}</h1>
        <p>{texto}</p>
        <div className="estado-acciones">
          <Link to="/" className="estado-btn">Volver al inicio</Link>
          {children}
        </div>
      </div>
    </main>
  );
}

export default PaginaEstado;
