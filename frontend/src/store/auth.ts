import { create } from "zustand";
import { persist } from "zustand/middleware";

export type UserSummary = {
  id: string;
  email: string;
  fullName: string;
  role: string;
};

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: UserSummary;
};

type AuthState = {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserSummary | null;
  setSession: (r: AuthResponse) => void;
  logout: () => void;
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setSession: (r) =>
        set({ accessToken: r.accessToken, refreshToken: r.refreshToken, user: r.user }),
      logout: () => set({ accessToken: null, refreshToken: null, user: null }),
    }),
    { name: "trb-auth" }
  )
);
