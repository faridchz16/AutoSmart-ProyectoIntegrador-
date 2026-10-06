import { Link } from "react-router-dom";
import "./Login.css";

function Register() {
  return (
    <div className="login-page">
      <div className="login-card">
        <h1 className="login-logo">🚗 AutoSmart</h1>
        <p className="login-subtitle">Crea tu cuenta (formulario en construcción)</p>

        <p className="login-footer">
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
        </p>
      </div>
    </div>
  );
}

export default Register;