import { useSyncExternalStore } from 'react';
import { useCurrentUser } from '../session/session';

// The people list's switchable columns, and which of them each user's screen has switched off. The
// choice belongs to the screen, not the sheet: it is kept in the browser per signed-in name, so it
// survives loading another sheet, and another user is never affected. A column switched off has no
// cell on the screen at all; every total and figure elsewhere is computed as before.
export const PEOPLE_COLUMNS = [
  'claimed', 'due-total', 'excess', 'statement-count', 'stated-total', 'statement-date', 'overdue-count',
  'overdue-total', 'patrons', 'backing', 'hold', 'retired', 'terms', 'contact', 'aliases',
] as const;

export type PeopleColumn = (typeof PEOPLE_COLUMNS)[number];

const storageKey = (user: string) => `people-columns-off:${user}`;
const listeners = new Set<() => void>();

function read(user: string): string {
  try {
    return localStorage.getItem(storageKey(user)) ?? '[]';
  } catch {
    return '[]';
  }
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** The columns switched off on this user's screen, and a setter for whether one is shown. */
export function useHiddenColumns(): [Set<string>, (column: PeopleColumn, shown: boolean) => void] {
  const [user] = useCurrentUser();
  const raw = useSyncExternalStore(subscribe, () => (user ? read(user) : '[]'));
  const off = new Set<string>(JSON.parse(raw) as string[]);
  const set = (column: PeopleColumn, shown: boolean) => {
    if (!user) return;
    const next = new Set(off);
    if (shown) next.delete(column); else next.add(column);
    try {
      localStorage.setItem(storageKey(user), JSON.stringify([...next].sort()));
    } catch { /* storage unavailable: nothing to keep */ }
    listeners.forEach((l) => l());
  };
  return [off, set];
}

/** Whether this user's screen shows the people list's column. */
export function useColumnShown(column: PeopleColumn): boolean {
  return !useHiddenColumns()[0].has(column);
}
