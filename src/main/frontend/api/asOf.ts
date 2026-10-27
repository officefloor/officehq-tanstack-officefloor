import { useSyncExternalStore } from 'react';

// The date this user's screen is shown as of (yyyy-mm-dd), '' when it shows the sheet as it is now. It
// belongs to this screen only: every request names it (X-As-Of, see http.ts), so the server answers a read
// as the store stood at the end of that date and refuses every change while it is set.
let asOf = '';
const listeners = new Set<() => void>();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** The as-of date the requests name now ('' for none, and while `withoutAsOf` runs). */
export function currentAsOf(): string {
  return ignoring > 0 ? '' : asOf;
}

// While above 0, requests made name no as-of date: they read the sheet as it is now.
let ignoring = 0;

/** Runs the work with the requests it makes (synchronously) reading the sheet as it is now, whatever the as-of date. */
export function withoutAsOf<T>(work: () => T): T {
  ignoring++;
  try {
    return work();
  } finally {
    ignoring--;
  }
}

export function setAsOf(date: string): void {
  if (asOf === date) return;
  asOf = date;
  listeners.forEach((l) => l());
}

/** The as-of date shown on this screen ('' for none). */
export function useAsOf(): string {
  return useSyncExternalStore(subscribe, () => asOf);
}
