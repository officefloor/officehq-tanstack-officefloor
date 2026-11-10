import { useSyncExternalStore } from 'react';

// A person's row shows at most one action error (person-error) at a time. Several slots in the row
// (statement, hold, retire) each take an action; the next action taken on the row, or a change to
// its fields, removes whatever error the row showed, whichever slot raised it.
type RowError = { owner: string; message: string };

const errors = new Map<string, RowError>();
const listeners = new Set<() => void>();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function set(personId: string, owner: string, message: string) {
  if (message === '') errors.delete(personId);
  else errors.set(personId, { owner, message });
  listeners.forEach((l) => l());
}

/** The row's error as this slot shows it ('' when it is none or another slot's), and its setter. */
export function useRowError(personId: string, owner: string): [string, (message: string) => void] {
  const current = useSyncExternalStore(subscribe, () => errors.get(personId));
  const error = current !== undefined && current.owner === owner ? current.message : '';
  return [error, (message: string) => set(personId, owner, message)];
}
