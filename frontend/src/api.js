import { useCallback, useEffect, useRef, useState } from "react";

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? "";
export const apiBaseUrl = configuredBaseUrl.replace(/\/+$/, "");
const TOKEN_KEY = "artconnect.token";

export function assetUrl(path) {
  return `${apiBaseUrl}${path}`;
}

export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // Storage can be unavailable (private mode); the session then lasts until reload.
  }
}

let unauthorizedHandler = null;

/** Registers a callback for when the API rejects the stored token (expired or logged out elsewhere). */
export function onUnauthorized(handler) {
  unauthorizedHandler = handler;
}

/**
 * Calls the Spring Boot API. Pass `json` to send a JSON body; the bearer token is attached automatically.
 * Throws an Error carrying the API's {"message"} on failure.
 */
export async function request(path, { json, headers, ...options } = {}) {
  const requestHeaders = new Headers(headers);
  const token = getToken();
  if (token) requestHeaders.set("Authorization", `Bearer ${token}`);
  let body = options.body;
  if (json !== undefined) {
    requestHeaders.set("Content-Type", "application/json");
    body = JSON.stringify(json);
  }

  const response = await fetch(`${apiBaseUrl}${path}`, { ...options, headers: requestHeaders, body });
  if (!response.ok) {
    if (response.status === 401 && token) unauthorizedHandler?.();
    let message = `Request failed (${response.status})`;
    try {
      const errorBody = await response.json();
      if (errorBody.message) message = errorBody.message;
    } catch {
      // Keep the HTTP status message when the response has no JSON body.
    }
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  if (response.status === 204) return null;
  return response.json();
}

/** Builds "?a=1&b=2", skipping empty values. */
export function queryString(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "" && value !== false) search.set(key, value);
  });
  const text = search.toString();
  return text ? `?${text}` : "";
}

/** Loads `path` (skipped when null) and exposes {data, loading, error, reload, setData}. Stale responses are ignored. */
export function useApi(path) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(Boolean(path));
  const [error, setError] = useState("");
  const latestRequest = useRef(0);

  const reload = useCallback(async ({ quiet = false } = {}) => {
    if (!path) return;
    const requestId = ++latestRequest.current;
    if (!quiet) setLoading(true);
    try {
      const result = await request(path);
      if (requestId === latestRequest.current) {
        setData(result);
        setError("");
      }
    } catch (requestError) {
      if (requestId === latestRequest.current) setError(requestError.message);
    } finally {
      if (requestId === latestRequest.current) setLoading(false);
    }
  }, [path]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, setData, loading, error, reload };
}

/** Image URLs from the API may be site-relative (sample data under /images); resolve those against the API origin. */
export function mediaUrl(url) {
  return url?.startsWith("/") ? assetUrl(url) : url;
}
