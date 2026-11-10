import { useSyncExternalStore } from 'react';

// The claims ticked for a bulk action, by sheet: each claim row ticks or unticks its own, and the
// bulk bar reads them all. Kept outside React so rows in different person rows share it.
const selected = new Map<string, ReadonlySet<string>>();
const listeners = new Set<() => void>();
const NONE: ReadonlySet<string> = new Set();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function set(sheetId: string, next: ReadonlySet<string>) {
  selected.set(sheetId, next);
  listeners.forEach((l) => l());
}

/** The ids of the claims ticked on the sheet. */
export function useSelectedClaims(sheetId: string): ReadonlySet<string> {
  return useSyncExternalStore(subscribe, () => selected.get(sheetId) ?? NONE);
}

/** Tick (or untick) a claim. */
export function selectClaim(sheetId: string, claimId: string, on: boolean) {
  const next = new Set(selected.get(sheetId) ?? NONE);
  if (on) next.add(claimId);
  else next.delete(claimId);
  set(sheetId, next);
}

/** Untick every claim on the sheet. */
export function clearSelectedClaims(sheetId: string) {
  set(sheetId, NONE);
}
