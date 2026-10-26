import { useSyncExternalStore } from 'react';

// One refusal per person row, shared by every cell that acts on the person (statement, retire, hold), so
// the row shows a single `person-error` and the next action on the row, whichever cell takes it, clears the last one.
const errors = new Map<number, string>();
const listeners = new Set<() => void>();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function setError(personId: number, error: string) {
  if ((errors.get(personId) ?? '') === error) return;
  if (error) errors.set(personId, error);
  else errors.delete(personId);
  listeners.forEach((l) => l());
}

/** The person's refusal ('' when none) and its setter. */
export function usePersonError(personId: number): [string, (error: string) => void] {
  const error = useSyncExternalStore(subscribe, () => errors.get(personId) ?? '');
  return [error, (next) => setError(personId, next)];
}
