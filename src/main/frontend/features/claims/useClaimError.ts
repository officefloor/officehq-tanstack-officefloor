import { useSyncExternalStore } from 'react';

// One refusal per claim row, shared by every cell that acts on the claim, so the row shows a single
// `claim-error` and the next action on the row, whichever cell takes it, clears the last one.
const errors = new Map<number, string>();
const listeners = new Set<() => void>();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** Sets a claim's refusal ('' clears it) from outside its row, e.g. a bulk action. */
export function setClaimError(claimId: number, error: string) {
  if ((errors.get(claimId) ?? '') === error) return;
  if (error) errors.set(claimId, error);
  else errors.delete(claimId);
  listeners.forEach((l) => l());
}

/** The claim's refusal ('' when none) and its setter. */
export function useClaimError(claimId: number): [string, (error: string) => void] {
  const error = useSyncExternalStore(subscribe, () => errors.get(claimId) ?? '');
  return [error, (next) => setClaimError(claimId, next)];
}
