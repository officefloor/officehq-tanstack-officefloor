import { useSyncExternalStore } from 'react';

// The people selected for a bulk action, per sheet. Kept outside React state because the checkboxes (on
// each person's row) and the bulk bar (on the sheet) are filled into different slots.
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

/** Select or clear one person on the sheet. */
export function setSelectedPerson(sheetId: string, personId: number, on: boolean) {
  const next = new Set(selected.get(sheetId) ?? NONE);
  if (on) next.add(personId);
  else next.delete(personId);
  selected.set(sheetId, next);
  notify();
}

/** Clear the sheet's selection of people. */
export function clearSelectedPeople(sheetId: string) {
  selected.delete(sheetId);
  notify();
}

/** The ids of the people selected on the sheet. */
export function useSelectedPeople(sheetId: string): Set<number> {
  return useSyncExternalStore(subscribe, () => selected.get(sheetId) ?? NONE);
}
