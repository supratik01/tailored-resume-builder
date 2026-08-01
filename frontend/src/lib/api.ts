import axios, { AxiosError, AxiosRequestConfig } from "axios";
import { useAuthStore } from "@/store/auth";

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const api = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers = config.headers ?? {};
    (config.headers as Record<string, string>).Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshInFlight: Promise<string | null> | null = null;

api.interceptors.response.use(
  (r) => r,
  async (error: AxiosError) => {
    const original = error.config as AxiosRequestConfig & { _retry?: boolean };
    if (error.response?.status === 401 && !original?._retry) {
      original._retry = true;
      const refreshToken = useAuthStore.getState().refreshToken;
      if (!refreshToken) {
        useAuthStore.getState().logout();
        return Promise.reject(error);
      }
      refreshInFlight ??= refresh(refreshToken).finally(() => (refreshInFlight = null));
      const newAccess = await refreshInFlight;
      if (!newAccess) {
        useAuthStore.getState().logout();
        return Promise.reject(error);
      }
      original.headers = original.headers ?? {};
      (original.headers as Record<string, string>).Authorization = `Bearer ${newAccess}`;
      return api(original);
    }
    return Promise.reject(error);
  }
);

async function refresh(refreshToken: string): Promise<string | null> {
  try {
    const r = await axios.post(`${baseURL}/api/auth/refresh`, { refreshToken });
    useAuthStore.getState().setSession(r.data);
    return r.data.accessToken as string;
  } catch {
    return null;
  }
}

export type ApiError = {
  status: number;
  error: string;
  message: string;
  fieldErrors?: { field: string; message: string }[];
};

export function apiErrorMessage(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const data = e.response?.data as ApiError | undefined;
    if (data?.message) return data.message;
    return e.message;
  }
  return e instanceof Error ? e.message : "Something went wrong";
}
