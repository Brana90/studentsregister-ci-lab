import { useState } from "react";
import api, { errMsg, setAuth } from "../api";

export default function Login({ onLogin }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const res = await api.post("/api/auth/login", { username, password });
      setAuth(res.data);
      onLogin(res.data);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-brand">
          <div className="login-logo">🎓</div>
          <h1>Student Register</h1>
          <p>Daily progress &amp; project tracking</p>
        </div>
        <form onSubmit={submit}>
          <label className="field">
            <span>Username (your email)</span>
            <input value={username} autoFocus autoComplete="username"
              onChange={(e) => setUsername(e.target.value)} required />
          </label>
          <label className="field">
            <span>Password</span>
            <input type="password" value={password} autoComplete="current-password"
              onChange={(e) => setPassword(e.target.value)} required />
          </label>
          {error && <div className="form-error">{error}</div>}
          <button className="btn btn-primary btn-block" disabled={busy}>
            {busy ? "Signing in…" : "Sign in"}
          </button>
        </form>
        <div className="login-hint">
          Students sign in with their email and the password given at
          registration.
        </div>
      </div>
    </div>
  );
}
