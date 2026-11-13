import { useSyncExternalStore } from 'react';
import { useCurrentSheet } from '../session/session';

// One error per person row, shared by every action on the row (a statement, a retirement, ...): the
// row shows it once, in its last cell, and the next action on the row clears it. An action whose error
// shows in its own cell (naming a patron) keeps it under its own kind; setting any error clears them all.
const errors = new Map<string, string>();
const listeners = new Set<() => void>();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** The person row's error of this kind ('' when none) and the setter every action on the row uses. */
export function usePersonError(personId: number, kind = ''): [string, (message: string) => void] {
  const [sheet] = useCurrentSheet();
  const row = `${sheet}:${personId}`;
  const key = kind ? `${row}:${kind}` : row;
  const error = useSyncExternalStore(subscribe, () => errors.get(key) ?? '');
  const setError = (message: string) => setPersonError(sheet, personId, message, kind);
  return [error, setError];
}

/** Set (or, with '', clear) a person row's error from outside the row, as a bulk action does. */
export function setPersonError(sheet: string, personId: number, message: string, kind = '') {
  const row = `${sheet}:${personId}`;
  const key = kind ? `${row}:${kind}` : row;
  for (const k of [...errors.keys()]) {
    if (k === row || k.startsWith(`${row}:`)) errors.delete(k);
  }
  if (message) errors.set(key, message);
  listeners.forEach((l) => l());
}

/** The person row's error, when there is one. */
export function PersonError({ personId }: { personId: number }) {
  const [error] = usePersonError(personId);
  return error ? <span data-testid="person-error">{error}</span> : null;
}
