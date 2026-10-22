import { useSyncExternalStore } from 'react';

// Why the server refused to lock a claim's editor (another user holds it open), per claim. Kept outside React state
// because it shows as the claim's error on its row, while the editor it belongs to is the form on the person's row:
// submitting or changing that form, like any next action on the row, clears it.
const refused = new Map<number, string>();
const listeners = new Set<() => void>();

function notify() {
  for (const l of listeners) l();
}

function subscribe(l: () => void) {
  listeners.add(l);
  return () => listeners.delete(l);
}

/** Record why the claim's lock was refused; '' clears it. */
export function setLockRefused(claimId: number, reason: string) {
  if ((refused.get(claimId) ?? '') === reason) return;
  if (reason === '') refused.delete(claimId);
  else refused.set(claimId, reason);
  notify();
}

/** Why the claim's lock was refused ('' when it was not). */
export function useLockRefused(claimId: number): string {
  return useSyncExternalStore(subscribe, () => refused.get(claimId) ?? '');
}
