import { useSyncExternalStore } from 'react';

// The date this user's screen previews the sheet at (yyyy-mm-dd), '' when it shows the sheet's real today. It
// belongs to this screen only: every request names it (X-Preview, see http.ts), so the server answers a read over
// the sheet's current records as if its today were that date; a change ignores it and uses the real today.
let preview = '';
const listeners = new Set<() => void>();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** The preview date the requests name now ('' for none). */
export function currentPreview(): string {
  return preview;
}

export function setPreview(date: string): void {
  if (preview === date) return;
  preview = date;
  listeners.forEach((l) => l());
}

/** The preview date shown on this screen ('' for none). */
export function usePreview(): string {
  return useSyncExternalStore(subscribe, () => preview);
}
