import PanelLayout from "./PanelLayout";
import { ENLACES_MECANICO } from "./enlacesMecanico";

/* Panel del mecánico (TEC2.1): agenda, citas, horarios, fechas bloqueadas y perfil.
   Usa la misma estructura que el panel del administrador. */

function MecanicoLayout() {
  return <PanelLayout enlaces={ENLACES_MECANICO} seccion="Mi taller" inicio="/mecanico" />;
}

export default MecanicoLayout;
