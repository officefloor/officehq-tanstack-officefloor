import { useSyncExternalStore } from 'react';

// The people ticked for a bulk action, kept per sheet outside React so each person's row and the bulk
// panel share it.
const selected = new Map<string, ReadonlySet<number>>();
const listeners = new Set<() => void>();
const NONE: ReadonlySet<number> = new Set();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function set(sheet: string, next: ReadonlySet<number>) {
  selected.set(sheet, next);
  listeners.forEach((l) => l());
}

/** The ids of the sheet's selected people. */
export function useSelectedPeople(sheet: string): ReadonlySet<number> {
  return useSyncExternalStore(subscribe, () => selected.get(sheet) ?? NONE);
}

/** Ticks or unticks a person. */
export function selectPerson(sheet: string, id: number, on: boolean) {
  const next = new Set(selected.get(sheet) ?? NONE);
  if (on) next.add(id);
  else next.delete(id);
  set(sheet, next);
}

/** Unticks every person on the sheet. */
export function clearSelectedPeople(sheet: string) {
  set(sheet, NONE);
}
