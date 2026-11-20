import axios from "axios";

// Dev: VITE_API_URL=http://localhost:8080 → talk to Spring Boot directly.
// Prod: empty → relative URLs, nginx proxies /api/ to the backend.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "",
});

const AUTH_KEY = "sr_auth";

export function getAuth() {
  try {
    return JSON.parse(localStorage.getItem(AUTH_KEY));
  } catch {
    return null;
  }
}

export function setAuth(auth) {
  if (auth) localStorage.setItem(AUTH_KEY, JSON.stringify(auth));
  else localStorage.removeItem(AUTH_KEY);
}

api.interceptors.request.use((config) => {
  const auth = getAuth();
  if (auth?.token) config.headers.Authorization = `Bearer ${auth.token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !err.config?.url?.includes("/auth/login")) {
      setAuth(null);
      window.location.reload();
    }
    return Promise.reject(err);
  }
);

export function errMsg(err) {
  const d = err.response?.data;
  return d?.message || d?.error || err.message || "Something went wrong";
}

export const fmtDate = (s) => {
  if (!s) return "-";
  const d = new Date(s);
  return isNaN(d) ? s : d.toLocaleDateString("en-IN", {
    day: "2-digit", month: "short", year: "numeric",
  });
};

export const fmtDateTime = (s) => {
  if (!s) return "-";
  const d = new Date(s);
  return isNaN(d) ? s : d.toLocaleString("en-IN", {
    day: "2-digit", month: "short",
    hour: "2-digit", minute: "2-digit",
  });
};

export const todayISO = () => new Date().toISOString().slice(0, 10);

export default api;
