import { useSyncExternalStore } from 'react';
import type { ReactNode } from 'react';

/** The people list's columns a user can switch off, in the order the chooser lists them. */
export const COLUMNS = [
  'claimed',
  'due-total',
  'excess',
  'statement-count',
  'stated-total',
  'statement-date',
  'overdue-count',
  'overdue-total',
  'patrons',
  'backing',
  'hold',
  'retired',
  'terms',
  'contact',
  'aliases',
] as const;
export type Column = (typeof COLUMNS)[number];

// Which columns each user has switched off. It belongs to the user's screen, not to the sheet:
// kept in this browser (per user), so loading a sheet leaves it as it is, and nothing is stored.
const hidden = new Map<string, Set<string>>();
const listeners = new Set<() => void>();
const storageKey = (user: string) => `columns-off:${user}`;

function offFor(user: string): Set<string> {
  let off = hidden.get(user);
  if (off === undefined) {
    try {
      off = new Set<string>(JSON.parse(localStorage.getItem(storageKey(user)) ?? '[]'));
    } catch {
      off = new Set<string>();
    }
    hidden.set(user, off);
  }
  return off;
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function setColumnShown(user: string, column: Column, shown: boolean): void {
  const off = new Set(offFor(user));
  if (shown) off.delete(column);
  else off.add(column);
  hidden.set(user, off);
  try {
    localStorage.setItem(storageKey(user), JSON.stringify([...off]));
  } catch {
    /* not kept past this page; still applies now */
  }
  listeners.forEach((l) => l());
}

/** Whether `user`'s screen shows `column` of the people list. */
export function useColumnShown(user: string, column: Column): boolean {
  return useSyncExternalStore(subscribe, () => !offFor(user).has(column));
}

/** Renders its header or cell only while `user`'s screen shows `column`. */
export function Col({ user, id, children }: { user: string; id: Column; children: ReactNode }) {
  return useColumnShown(user, id) ? <>{children}</> : null;
}
