import { useCallback, useEffect, useRef, useState } from "react";
import { listarMateriales, textoUnidad, formatoSoles, formatoCantidad } from "../../services/inventarioService";
import Modal from "../../components/admin/Modal";
import Paginacion from "../../components/admin/Paginacion";
import Aviso from "../../components/admin/Aviso";
import FormMaterial from "./FormMaterial";
import FormIngreso from "./FormIngreso";
import "../../components/admin/admin-ui.css";
import "./Inventario.css";

/* T57.2 – Inventario del almacén: catálogo de materiales y repuestos (API /admin/materials de T57.1).
   - Tabla con código, nombre, unidad, stock (con su mínimo), costo unitario y valor en almacén
   - Búsqueda por código o nombre
   - Registrar y editar con formulario (código único, costo ≥ 0, stock mínimo)
   El stock no se edita aquí: cambia con los ingresos (T58) y las salidas del almacén (T51 y T59).
   T58.2: botón «Ingreso» por material: formulario de ingreso de mercadería y su kardex. */

const TAMANO = 10;

const sinTildes = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();

const IconoIngreso = () => (
  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M12 4v11M7.5 10.5 12 15l4.5-4.5" />
    <path d="M4 15v3.5A1.5 1.5 0 0 0 5.5 20h13a1.5 1.5 0 0 0 1.5-1.5V15" />
  </svg>
);

const IconoEditar = () => (
  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M4 20h4L18.5 9.5a2.1 2.1 0 0 0-4-4L4 16v4Z" />
    <path d="m13.5 6.5 4 4" />
  </svg>
);

function Inventario() {
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, materiales: [], error: "" });
  const [busqueda, setBusqueda] = useState("");
  const [pagina, setPagina] = useState(0);
  const [formulario, setFormulario] = useState(null); // { material: null | {...} }
  const [ingreso, setIngreso] = useState(null); // material al que se registra un ingreso
  const [aviso, setAviso] = useState(null);

  const cargando = resultado.clave !== recarga;
  const primeraCarga = resultado.clave === null;

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  useEffect(() => {
    let vigente = true;
    listarMateriales()
      .then((materiales) => vigente && setResultado({ clave: recarga, materiales, error: "" }))
      .catch((e) => vigente && setResultado({ clave: recarga, materiales: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recarga]);

  const { materiales } = resultado;
  const texto = sinTildes(busqueda.trim());
  const filtrados = materiales
    .filter((m) => !texto || sinTildes(`${m.codigo} ${m.nombre}`).includes(texto))
    .sort((a, b) => a.codigo.localeCompare(b.codigo, "es", { numeric: true }));
  const paginas = Math.max(1, Math.ceil(filtrados.length / TAMANO));
  const paginaActual = Math.min(pagina, paginas - 1);
  const visibles = filtrados.slice(paginaActual * TAMANO, paginaActual * TAMANO + TAMANO);
  const valorStock = materiales.reduce((total, m) => total + m.stockActual * m.costoUnitario, 0);
  const codigosUsados = materiales.map((m) => m.codigo);

  const alGuardar = (guardado, editando) => {
    setFormulario(null);
    if (editando) {
      setResultado((r) => ({ ...r, materiales: r.materiales.map((m) => (m.id === guardado.id ? guardado : m)) }));
      avisar("ok", `Se guardaron los cambios de ${guardado.codigo} · "${guardado.nombre}".`);
    } else {
      setResultado((r) => ({ ...r, materiales: [...r.materiales, guardado] }));
      setBusqueda(guardado.codigo);
      setPagina(0);
      avisar("ok", `Se registró ${guardado.codigo} · "${guardado.nombre}" con stock 0.`);
    }
  };

  const alIngresar = (actualizado, mov) => {
    setIngreso(null);
    setResultado((r) => ({ ...r, materiales: r.materiales.map((m) => (m.id === actualizado.id ? { ...m, ...actualizado } : m)) }));
    avisar(
      "ok",
      `Ingresaron ${formatoCantidad(mov.cantidad)} ${textoUnidad(actualizado.unidad, mov.cantidad)} de ${actualizado.nombre}. Stock: ${formatoCantidad(mov.stockResultante)}.`
    );
  };

  return (
    <section className="gestion inv">
      <div className="gestion-cabecera">
        <div>
          <h2>Inventario</h2>
          <p>
            Materiales y repuestos del almacén.
            {!primeraCarga && !resultado.error && (
              <>
                {" "}
                <strong>{materiales.length}</strong> {materiales.length === 1 ? "material" : "materiales"} · valor del stock{" "}
                <strong>{formatoSoles(valorStock)}</strong>.
              </>
            )}
          </p>
        </div>
        <button type="button" className="btn btn-pri" onClick={() => setFormulario({ material: null })}>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          Nuevo material
        </button>
      </div>

      {/* ---------- Búsqueda ---------- */}
      <div className="filtros">
        <label className="buscador">
          <span className="sr-only">Buscar por código o nombre</span>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input
            type="search"
            placeholder="Buscar por código o nombre"
            value={busqueda}
            onChange={(e) => {
              setBusqueda(e.target.value);
              setPagina(0);
            }}
          />
        </label>
        {texto && !primeraCarga && !resultado.error && (
          <span className="inv-encontrados" role="status">
            {filtrados.length} de {materiales.length}
          </span>
        )}
      </div>

      {/* ---------- Tabla ---------- */}
      <div className={cargando && !primeraCarga ? "tabla-caja actualizando" : "tabla-caja"} aria-busy={cargando}>
        {resultado.error ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudo cargar el inventario</p>
            <p>{resultado.error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        ) : !primeraCarga && filtrados.length === 0 ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">{materiales.length ? "Sin resultados" : "El almacén está vacío"}</p>
            <p>
              {materiales.length
                ? `Ningún material coincide con «${busqueda.trim()}».`
                : "Registra el primer material con el botón «Nuevo material»."}
            </p>
          </div>
        ) : (
          <table className="tabla inv-tabla">
            <thead>
              <tr>
                <th scope="col">Código</th>
                <th scope="col">Material</th>
                <th scope="col" className="col-num">Stock</th>
                <th scope="col" className="col-num">Costo unit.</th>
                <th scope="col" className="col-num">Valor en almacén</th>
                <th scope="col"><span className="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {primeraCarga
                ? Array.from({ length: 6 }, (_, i) => (
                    <tr key={i} className="fila-esqueleto" aria-hidden="true">
                      <td><span className="esq" /></td>
                      <td><span className="esq esq-largo" /></td>
                      <td><span className="esq" /></td>
                      <td><span className="esq" /></td>
                      <td><span className="esq" /></td>
                      <td />
                    </tr>
                  ))
                : visibles.map((m) => (
                    <tr key={m.id}>
                      <td data-label="Código">
                        <span className="inv-codigo">{m.codigo}</span>
                      </td>
                      <td data-label="Material">
                        <div className="servicio-info">
                          <strong>{m.nombre}</strong>
                          <span>Se mide por {textoUnidad(m.unidad, 1)}</span>
                        </div>
                      </td>
                      <td data-label="Stock" className="col-num">
                        <span className="inv-stock">
                          <strong>{formatoCantidad(m.stockActual)}</strong> {textoUnidad(m.unidad, m.stockActual)}
                          <small>mín. {formatoCantidad(m.stockMinimo)}</small>
                        </span>
                      </td>
                      <td data-label="Costo unit." className="col-num">
                        <span className="precio">{formatoSoles(m.costoUnitario)}</span>
                        <small className="inv-por">por {textoUnidad(m.unidad, 1)}</small>
                      </td>
                      <td data-label="Valor en almacén" className="col-num">
                        <span className="inv-valor">{formatoSoles(m.stockActual * m.costoUnitario)}</span>
                      </td>
                      <td data-label="Acciones" className="col-acciones">
                        <button
                          type="button"
                          className="btn-icono inv-btn-ingreso"
                          onClick={() => setIngreso(m)}
                          aria-label={`Registrar ingreso de ${m.codigo} ${m.nombre}`}
                        >
                          <IconoIngreso />
                          <span>Ingreso</span>
                        </button>
                        <button
                          type="button"
                          className="btn-icono"
                          onClick={() => setFormulario({ material: m })}
                          aria-label={`Editar ${m.codigo} ${m.nombre}`}
                        >
                          <IconoEditar />
                          <span>Editar</span>
                        </button>
                      </td>
                    </tr>
                  ))}
            </tbody>
          </table>
        )}
      </div>

      {!resultado.error && (
        <Paginacion pagina={paginaActual} paginas={paginas} total={filtrados.length} tamano={TAMANO} alCambiar={setPagina} />
      )}

      {/* ---------- Registrar / editar ---------- */}
      <Modal
        abierto={!!formulario}
        alCerrar={() => setFormulario(null)}
        titulo={formulario?.material ? "Editar material" : "Nuevo material"}
      >
        {formulario && (
          <FormMaterial
            material={formulario.material}
            codigosUsados={codigosUsados}
            alGuardar={alGuardar}
            alCerrar={() => setFormulario(null)}
          />
        )}
      </Modal>

      {/* ---------- Ingreso de mercadería (T58.2) ---------- */}
      <Modal abierto={!!ingreso} alCerrar={() => setIngreso(null)} titulo="Registrar ingreso">
        {ingreso && <FormIngreso material={ingreso} alGuardar={alIngresar} alCerrar={() => setIngreso(null)} />}
      </Modal>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Inventario;
