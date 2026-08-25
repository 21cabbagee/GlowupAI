"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { api, type Profile } from "./api";

const USER_KEY = "skinproof_user_id";

interface SessionValue {
  userId: string | null;
  profile: Profile | null;
  /** False only until the persisted id has been read and the profile fetched. */
  ready: boolean;
  isPremium: boolean;
  hasConsent: boolean;
  signIn: (userId: string) => void;
  signOut: () => void;
  refresh: () => Promise<void>;
}

const SessionContext = createContext<SessionValue | null>(null);

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [userId, setUserId] = useState<string | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [ready, setReady] = useState(false);

  // Hydrate from localStorage after mount so the server render stays stable.
  useEffect(() => {
    const storedUser = localStorage.getItem(USER_KEY);
    if (!storedUser) {
      setReady(true);
      return;
    }
    setUserId(storedUser);
    api
      .profile(storedUser)
      .then(setProfile)
      .catch(() => {
        // The id is stale (database reset or account deleted) — drop it.
        localStorage.removeItem(USER_KEY);
        setUserId(null);
      })
      .finally(() => setReady(true));
  }, []);

  const refresh = useCallback(async () => {
    if (!userId) return;
    setProfile(await api.profile(userId));
  }, [userId]);

  const signIn = useCallback((id: string) => {
    localStorage.setItem(USER_KEY, id);
    setUserId(id);
    api.profile(id).then(setProfile).catch(() => undefined);
  }, []);

  const signOut = useCallback(() => {
    localStorage.removeItem(USER_KEY);
    setUserId(null);
    setProfile(null);
  }, []);

  const value = useMemo<SessionValue>(
    () => ({
      userId,
      profile,
      ready,
      isPremium: profile?.entitlement.plan === "premium",
      hasConsent: profile?.user.consent_state === "active",
      signIn,
      signOut,
      refresh,
    }),
    [userId, profile, ready, signIn, signOut, refresh],
  );

  return (
    <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
  );
}

export function useSession(): SessionValue {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("useSession must be used inside SessionProvider");
  return ctx;
}
