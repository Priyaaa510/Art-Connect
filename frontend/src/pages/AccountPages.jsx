import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { request } from "../api.js";
import { useAuth } from "../auth.jsx";
import { Avatar } from "../components.jsx";

export function LoginPage() {
  return <AuthForm mode="login" />;
}

export function RegisterPage() {
  return <AuthForm mode="register" />;
}

function AuthForm({ mode }) {
  const isRegister = mode === "register";
  const { authenticate } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [role, setRole] = useState("COLLECTOR");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    const formData = new FormData(event.currentTarget);
    try {
      const user = await authenticate(mode, {
        username: formData.get("username"),
        password: formData.get("password"),
        ...(isRegister && { role, displayName: formData.get("displayName") }),
      });
      const fallback = user.role === "ARTIST" && isRegister ? "/studio" : "/";
      navigate(location.state?.from ?? fallback, { replace: true });
    } catch (requestError) {
      setError(requestError.message);
      setSubmitting(false);
    }
  }

  return (
    <main className="center-box">
      <section className="card p-4 auth-card">
        <h1 className="auth-title">{isRegister ? "Join ArtConnect" : "Welcome back"}</h1>
        <p className="auth-subtitle">
          {isRegister ? "Collect original art, or open a studio and sell your own." : "Log in to buy, bid and manage your collection."}
        </p>
        {location.state?.message && <p className="auth-notice" role="status">{location.state.message}</p>}
        <form onSubmit={submit}>
          {isRegister && (
            <fieldset className="mb-3">
              <legend className="form-label text-dark fs-6">I want to</legend>
              <div className="role-options">
                <label className={role === "COLLECTOR" ? "active" : ""}>
                  <input type="radio" name="role" value="COLLECTOR" checked={role === "COLLECTOR"} onChange={() => setRole("COLLECTOR")} />
                  Collect art
                </label>
                <label className={role === "ARTIST" ? "active" : ""}>
                  <input type="radio" name="role" value="ARTIST" checked={role === "ARTIST"} onChange={() => setRole("ARTIST")} />
                  Sell my art
                </label>
              </div>
            </fieldset>
          )}
          <div className="mb-3">
            <label className="form-label text-dark" htmlFor="username">Username</label>
            <input className="form-control" id="username" name="username" autoComplete="username" required minLength={isRegister ? 3 : undefined} maxLength={30} />
          </div>
          {isRegister && (
            <div className="mb-3">
              <label className="form-label text-dark" htmlFor="displayName">Display name <small className="text-secondary">(optional)</small></label>
              <input className="form-control" id="displayName" name="displayName" maxLength={60} />
            </div>
          )}
          <div className="mb-3">
            <label className="form-label text-dark" htmlFor="password">Password</label>
            <input className="form-control" id="password" name="password" type="password" autoComplete={isRegister ? "new-password" : "current-password"} required minLength={isRegister ? 6 : undefined} />
          </div>
          {error && <p className="text-danger" role="alert">{error}</p>}
          <button className="btn btn-primary btn-lg w-100" type="submit" disabled={submitting}>
            {submitting ? "Please wait..." : isRegister ? "Create account" : "Log in"}
          </button>
        </form>
        <p className="mt-3 mb-0 text-center text-dark">
          {isRegister ? "Already have an account? " : "Don't have an account? "}
          <Link to={isRegister ? "/login" : "/register"} state={location.state}>{isRegister ? "Log in" : "Register"}</Link>
        </p>
        <p className="mt-2 mb-0 text-center"><Link to="/">← Back to the gallery</Link></p>
      </section>
    </main>
  );
}

export function ProfilePage() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({
    displayName: user.displayName ?? "",
    bio: user.bio ?? "",
    location: user.location ?? "",
    website: user.website ?? "",
    becomeArtist: false,
  });
  const [status, setStatus] = useState(null);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);

  const change = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    setStatus(null);
    try {
      const updated = await request("/api/me", { method: "PUT", json: form });
      setUser(updated);
      setForm((current) => ({ ...current, becomeArtist: false }));
      setStatus({ ok: true, text: "Profile saved." });
    } catch (requestError) {
      setStatus({ ok: false, text: requestError.message });
    } finally {
      setSaving(false);
    }
  }

  async function uploadAvatar(event) {
    const file = event.target.files[0];
    if (!file) return;
    setUploading(true);
    setStatus(null);
    const body = new FormData();
    body.append("image", file);
    try {
      setUser(await request("/api/me/avatar", { method: "POST", body }));
    } catch (requestError) {
      setStatus({ ok: false, text: requestError.message });
    } finally {
      setUploading(false);
      event.target.value = "";
    }
  }

  const isArtist = user.role === "ARTIST" || user.role === "ADMIN";

  return (
    <section className="container page-section">
      <h2>Your profile</h2>
      <form className="glass-panel form-panel" onSubmit={save}>
        <div className="avatar-editor">
          <Avatar src={user.avatarUrl} name={user.displayName} size={96} />
          <div>
            <label className="btn btn-outline-dark btn-sm" htmlFor="avatar">{uploading ? "Uploading..." : "Change photo"}</label>
            <input id="avatar" type="file" accept="image/*" className="visually-hidden" onChange={uploadAvatar} disabled={uploading} />
            <p className="mb-0 mt-1"><small>@{user.username} · {isArtist ? "Artist" : "Collector"} account</small></p>
            {isArtist && <Link to={`/artists/${user.id}`}><small>View public profile</small></Link>}
          </div>
        </div>

        <label className="form-label" htmlFor="displayName">Display name</label>
        <input id="displayName" className="form-control mb-3" required maxLength={60} value={form.displayName} onChange={change("displayName")} />
        <label className="form-label" htmlFor="bio">Bio</label>
        <textarea id="bio" className="form-control mb-3" rows="4" maxLength={1000} placeholder="Tell collectors about yourself and your work" value={form.bio} onChange={change("bio")} />
        <div className="row g-3 mb-3">
          <div className="col-sm-6">
            <label className="form-label" htmlFor="location">Location</label>
            <input id="location" className="form-control" maxLength={100} value={form.location} onChange={change("location")} />
          </div>
          <div className="col-sm-6">
            <label className="form-label" htmlFor="website">Website</label>
            <input id="website" className="form-control" type="url" maxLength={200} placeholder="https://" value={form.website} onChange={change("website")} />
          </div>
        </div>
        {!isArtist && (
          <label className="form-check mb-3">
            <input type="checkbox" className="form-check-input" checked={form.becomeArtist}
              onChange={(event) => setForm((current) => ({ ...current, becomeArtist: event.target.checked }))} />
            <span className="form-check-label">Upgrade to an artist account so I can sell my work and curate exhibitions</span>
          </label>
        )}
        {status && <p className={status.ok ? "text-success-light" : "text-danger"} role="status">{status.text}</p>}
        <button className="btn btn-primary" type="submit" disabled={saving}>{saving ? "Saving..." : "Save profile"}</button>
      </form>
    </section>
  );
}
