import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import { getToken, onUnauthorized, request, setToken } from "./api.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(() => !getToken());

  useEffect(() => {
    onUnauthorized(() => {
      setToken(null);
      setUser(null);
    });
    if (!getToken()) return;
    request("/api/me")
      .then(setUser)
      .catch(() => setToken(null))
      .finally(() => setReady(true));
  }, []);

  /** mode is "login" or "register". */
  const authenticate = useCallback(async (mode, payload) => {
    const result = await request(`/api/auth/${mode}`, { method: "POST", json: payload });
    setToken(result.token);
    setUser(result.user);
    return result.user;
  }, []);

  const logout = useCallback(async () => {
    try {
      await request("/api/auth/logout", { method: "POST" });
    } catch {
      // The local session is cleared either way.
    }
    setToken(null);
    setUser(null);
  }, []);

  const value = useMemo(() => ({
    user,
    ready,
    isArtist: user?.role === "ARTIST" || user?.role === "ADMIN",
    authenticate,
    logout,
    setUser,
  }), [user, ready, authenticate, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}

/** Sends visitors to the login page (and back afterwards). With `artist`, also requires an artist account. */
export function RequireAuth({ artist = false, children }) {
  const { user, ready, isArtist } = useAuth();
  const location = useLocation();

  if (!ready) return <p className="page-state container" role="status">Loading...</p>;
  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location.pathname + location.search, message: "Please log in to continue." }}
      />
    );
  }
  if (artist && !isArtist) {
    return (
      <section className="container page-section">
        <div className="glass-panel">
          <h2>Artist accounts only</h2>
          <p>Switch your profile to an artist account to upload artwork and curate exhibitions.</p>
          <Link to="/profile" className="btn btn-primary">Edit profile</Link>
        </div>
      </section>
    );
  }
  return children;
}
