import { useEffect, useState } from "react";
import { cargarFoto } from "../../services/ordenesService";

/* Foto de evidencia protegida: se descarga con el token del cliente (GET /photos/{id}) y se muestra al terminar.
   Estilos en Seguimiento.css (seg-foto-img). */
function FotoCliente({ foto, alt }) {
  const [estado, setEstado] = useState({ clave: null, url: "", error: false });
  useEffect(() => {
    let vigente = true;
    cargarFoto(foto)
      .then((url) => vigente && setEstado({ clave: foto.clave, url, error: false }))
      .catch(() => vigente && setEstado({ clave: foto.clave, url: "", error: true }));
    return () => {
      vigente = false;
    };
  }, [foto]);
  if (estado.clave !== foto.clave) return <span className="seg-foto-img esq" aria-hidden="true" />;
  if (estado.error) return <span className="seg-foto-img seg-foto-falla">Sin imagen</span>;
  return <img className="seg-foto-img" src={estado.url} alt={alt} />;
}

export default FotoCliente;
