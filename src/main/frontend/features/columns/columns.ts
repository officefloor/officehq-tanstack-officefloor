import { useCallback, useSyncExternalStore } from 'react';

/** The people list's columns a user may switch off, in the chooser's order. Each names the cell `person-<column>`. */
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

// Which columns are switched off is a choice of each user's screen: kept in this browser, per signed-in user, and
// never sent to the server. Switching one off removes its cells only; no total or other figure changes.
const storageKey = (user: string) => `people-columns-off:${user}`;
const listeners = new Set<() => void>();

function readOff(user: string): string {
  try {
    return localStorage.getItem(storageKey(user)) ?? '[]';
  } catch {
    return '[]';
  }
}

export function setColumnShown(user: string, column: Column, shown: boolean) {
  const off = new Set<string>(JSON.parse(readOff(user)) as string[]);
  if (shown) off.delete(column);
  else off.add(column);
  try {
    localStorage.setItem(storageKey(user), JSON.stringify([...off].sort()));
  } catch {
    /* storage unavailable: the choice is lost */
  }
  listeners.forEach((l) => l());
}

const subscribe = (l: () => void) => {
  listeners.add(l);
  return () => listeners.delete(l);
};

/** Whether `user`'s screen shows a column of the people list. */
export function useShownColumns(user: string): (column: Column) => boolean {
  const raw = useSyncExternalStore(subscribe, () => readOff(user));
  return useCallback((column: Column) => !(JSON.parse(raw) as string[]).includes(column), [raw]);
}
