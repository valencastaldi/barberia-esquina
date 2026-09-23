import { useEffect, useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useSesion } from "../sesion.jsx";

/** RF-08: ingreso al panel. */
export default function Login() {
  const { sesion, iniciar } = useSesion();
  const navegar = useNavigate();
  const { state } = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(state?.motivo ?? null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    document.body.className = "admin login";
    document.title = "Ingresar — Barbería Esquina";
  }, []);

  if (sesion) return <Navigate to={state?.desde ?? "/admin/agenda"} replace />;

  async function ingresar(e) {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError("Completá el email y la contraseña.");
      return;
    }
    setEnviando(true);
    setError(null);
    try {
      await iniciar(email.trim(), password);
      navegar(state?.desde ?? "/admin/agenda", { replace: true });
    } catch (err) {
      setError(err.message);
      setEnviando(false);
    }
  }

  return (
    <main className="caja-login">
      <img className="sello" src="/logo.jpg" alt="Barbería Esquina 1290" />
      <h1>Panel de gestión</h1>
      <p>Ingresá con tu cuenta para ver la agenda del día.</p>

      {error && <div className="aviso aviso-error-login" role="alert">{error}</div>}

      <form onSubmit={ingresar} noValidate>
        <label className="campo">
          <span>Email</span>
          <input type="email" autoComplete="username" placeholder="tu@barberiaesquina.com" autoFocus
                 value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label className="campo">
          <span>Contraseña</span>
          <input type="password" autoComplete="current-password" placeholder="••••••••"
                 value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>
        <button type="submit" className="btn btn-primario btn-block" style={{ marginTop: 8 }} disabled={enviando}>
          {enviando ? "Ingresando…" : "Ingresar"}
        </button>
      </form>

      <p className="pie-login"><Link to="/">← Volver al sitio</Link></p>
    </main>
  );
}
