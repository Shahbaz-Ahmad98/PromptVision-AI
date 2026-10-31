
import { useState } from "react";

export default function AuthPanel({ onLogin }) {
  const [mode, setMode] = useState("login");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // ==========================================
  // LOGIN AND REGISTRATION
  // ==========================================

  async function handleSubmit(e) {
    e.preventDefault();

    if (loading) return;

    setLoading(true);
    setError("");
    setMessage("");

    const isLogin = mode === "login";

    const endpoint = isLogin
      ? "/api/auth/login"
      : "/api/auth/register";

    const normalizedEmail = email.trim().toLowerCase();

    const payload = isLogin
      ? {
          email: normalizedEmail,
          password,
        }
      : {
          name: name.trim(),
          email: normalizedEmail,
          password,
        };

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      // Helpful while troubleshooting
      console.log(
        "Authentication request:",
        endpoint,
        "Status:",
        response.status
      );

      if (!response.ok) {
        if (response.status === 409) {
          throw new Error(
            "This email is already registered. Please login."
          );
        }

        if (response.status === 401) {
          throw new Error(
            "Login failed. Check your email and password."
          );
        }

        if (response.status === 403) {
          throw new Error(
            "Access denied (403). Check the server security or CORS configuration."
          );
        }

        if (response.status === 400) {
          throw new Error(
            "Invalid input. Please check your details."
          );
        }

        throw new Error(
          `Authentication request failed (${response.status}).`
        );
      }

      if (!isLogin) {
        setMessage(
          "Registration successful! Please login."
        );
        setMode("login");
        setPassword("");
        return;
      }

      const data = await response.json();

      if (!data.token) {
        throw new Error(
          "Login succeeded, but no JWT token was returned."
        );
      }

      // Send authentication data to App.jsx
      onLogin(data);

    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Something went wrong."
      );

    } finally {
      setLoading(false);
    }
  }

  // ==========================================
  // SWITCH LOGIN / REGISTER
  // ==========================================

  function switchMode() {
    setMode((current) =>
      current === "login" ? "register" : "login"
    );

    setPassword("");
    setError("");
    setMessage("");
  }

  // ==========================================
  // UI
  // ==========================================

  return (
    <div className="auth-panel">

      <h2>
        {mode === "login"
          ? "Welcome Back"
          : "Create Account"}
      </h2>

      <p>
        {mode === "login"
          ? "Login to access Spring AI Studio"
          : "Register to get started"}
      </p>

      <form
        onSubmit={handleSubmit}
        autoComplete="on"
      >

        {mode === "register" && (
          <input
            type="text"
            name="name"
            autoComplete="name"
            placeholder="Full Name"
            value={name}
            onChange={(e) =>
              setName(e.target.value)
            }
            required
          />
        )}

        <input
          type="email"
          name="email"
          autoComplete="username"
          placeholder="Email Address"
          value={email}
          onChange={(e) =>
            setEmail(e.target.value)
          }
          required
        />

        <input
          type="password"
          name="password"
          autoComplete={
            mode === "login"
              ? "current-password"
              : "new-password"
          }
          placeholder="Password"
          value={password}
          onChange={(e) =>
            setPassword(e.target.value)
          }
          required
          minLength={
            mode === "register" ? 8 : undefined
          }
        />

        <button
          type="submit"
          disabled={loading}
        >
          {loading
            ? "Please wait..."
            : mode === "login"
            ? "Login"
            : "Register"}
        </button>

      </form>

      {message && (
        <p className="auth-success" role="status">
          {message}
        </p>
      )}

      {error && (
        <p className="auth-error" role="alert">
          {error}
        </p>
      )}

      <p>
        {mode === "login"
          ? "Don't have an account?"
          : "Already have an account?"}
      </p>

      <button
        type="button"
        onClick={switchMode}
        disabled={loading}
      >
        {mode === "login"
          ? "Create Account"
          : "Login Instead"}
      </button>

    </div>
  );
}
