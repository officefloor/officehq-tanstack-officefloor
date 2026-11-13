import { useSyncExternalStore } from 'react';
import { useCurrentSheet } from '../session/session';

// One error per person row, shared by every action on the row (a statement, a retirement, ...): the
// row shows it once, in its last cell, and the next action on the row clears it.
const errors = new Map<string, string>();
const listeners = new Set<() => void>();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** The person row's error ('' when none) and the setter every action on the row uses. */
export function usePersonError(personId: number): [string, (message: string) => void] {
  const [sheet] = useCurrentSheet();
  const key = `${sheet}:${personId}`;
  const error = useSyncExternalStore(subscribe, () => errors.get(key) ?? '');
  const setError = (message: string) => {
    if (message) errors.set(key, message);
    else errors.delete(key);
    listeners.forEach((l) => l());
  };
  return [error, setError];
}

/** The person row's error, when there is one. */
export function PersonError({ personId }: { personId: number }) {
  const [error] = usePersonError(personId);
  return error ? <span data-testid="person-error">{error}</span> : null;
}
