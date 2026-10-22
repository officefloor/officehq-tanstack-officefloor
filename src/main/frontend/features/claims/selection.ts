import { useSyncExternalStore } from 'react';

// The claims selected for a bulk action, per sheet. Kept outside React state because the checkboxes (on
// each claim's row) and the bulk buttons (on the sheet) are filled into different slots.
const selected = new Map<string, Set<number>>();
const listeners = new Set<() => void>();
const NONE: Set<number> = new Set();

function notify() {
  for (const l of listeners) l();
}

function subscribe(l: () => void) {
  listeners.add(l);
  return () => listeners.delete(l);
}

/** Select or clear one claim on the sheet. */
export function setSelected(sheetId: string, claimId: number, on: boolean) {
  const next = new Set(selected.get(sheetId) ?? NONE);
  if (on) next.add(claimId);
  else next.delete(claimId);
  selected.set(sheetId, next);
  notify();
}

/** Clear the sheet's selection. */
export function clearSelected(sheetId: string) {
  selected.delete(sheetId);
  notify();
}

/** The ids of the claims selected on the sheet. */
export function useSelectedClaims(sheetId: string): Set<number> {
  return useSyncExternalStore(subscribe, () => selected.get(sheetId) ?? NONE);
}
