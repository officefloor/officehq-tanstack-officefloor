import { useSyncExternalStore } from 'react';
import { useCurrentSheet } from '../session/session';

// One error per claim row, shared by every action on the row (a status change, a move, the edit button, ...): the
// row shows it once, and the next action on the row clears it.
const errors = new Map<string, string>();
const listeners = new Set<() => void>();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** The claim row's error ('' when none) and the setter every action on the row uses. */
export function useClaimError(claimId: number): [string, (message: string) => void] {
  const [sheet] = useCurrentSheet();
  const key = `${sheet}:${claimId}`;
  const error = useSyncExternalStore(subscribe, () => errors.get(key) ?? '');
  return [error, (message: string) => setClaimError(sheet, claimId, message)];
}

/** Set (or, with '', clear) the error of a claim row on the sheet. */
export function setClaimError(sheet: string, claimId: number, message: string) {
  const key = `${sheet}:${claimId}`;
  if (message) errors.set(key, message);
  else errors.delete(key);
  listeners.forEach((l) => l());
}
