
import React, { useState } from "react";
export default function Login({ onLogin, error, busy }) {
  const [username, setUsername] = useState("student1");
  const [password, setPassword] = useState("student123");

  function submit(event) {
    event.preventDefault();
    onLogin(username.trim(), password);
  }

  return (
    <div className="loginPage">
      <div className="loginBrand">◆ Campus Desk</div>

      <div className="card loginCard">
        <div className="eyebrow">STUDENT SUPPORT</div>
        <h1>Welcome back</h1>
        <p>Sign in to manage campus requests.</p>

        <form onSubmit={submit}>
          <label>
            Username
            <input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
              required
            />
          </label>

          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              required
            />
          </label>

          <button disabled={busy}>{busy ? "Signing in..." : "Sign in"}</button>
        </form>

        {error && <p className="error">{error}</p>}

        <small>
          Demo: student1 / student123 · staff1 / staff123 · manager / manager123
        </small>
      </div>
    </div>
  );
}
