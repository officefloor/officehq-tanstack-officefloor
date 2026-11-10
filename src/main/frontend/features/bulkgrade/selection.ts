import { useSyncExternalStore } from 'react';

// The people ticked for a bulk grade, by sheet: each person row ticks or unticks its own, and the
// bulk grade bar reads them all. Kept outside React so the rows and the bar share it.
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

/** The ids of the people ticked on the sheet. */
export function useSelectedPeople(sheetId: string): ReadonlySet<string> {
  return useSyncExternalStore(subscribe, () => selected.get(sheetId) ?? NONE);
}

/** Tick (or untick) a person. */
export function selectPerson(sheetId: string, personId: string, on: boolean) {
  const next = new Set(selected.get(sheetId) ?? NONE);
  if (on) next.add(personId);
  else next.delete(personId);
  set(sheetId, next);
}

/** Untick every person on the sheet. */
export function clearSelectedPeople(sheetId: string) {
  set(sheetId, NONE);
}
