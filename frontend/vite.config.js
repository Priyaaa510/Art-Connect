import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  // Where the Spring Boot API runs in development; override in frontend/.env.local
  const env = loadEnv(mode, process.cwd(), "");
  const apiUrl = env.API_PROXY_TARGET || "http://localhost:8083";

  /**
   * In development the browser only talks to the Vite server, so API calls are same-origin.
   * Dropping the Origin header stops the backend's CORS check from rejecting them when Vite
   * runs on another port (e.g. 5174) or is opened as 127.0.0.1.
   */
  const toApi = {
    target: apiUrl,
    configure: (proxy) => {
      proxy.on("proxyReq", (proxyRequest) => proxyRequest.removeHeader("origin"));
    },
  };

  return {
    plugins: [react()],
    server: {
      proxy: {
        "/api": toApi,
        "/images": toApi,
        "/videos": toApi,
      },
    },
  };
});
