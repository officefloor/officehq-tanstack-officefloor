import { useRef, useSyncExternalStore } from 'react';

// The last action taken on each row, by row key. A row's cells come from separate slots, each with its own
// mutation; an error belongs to the action that raised it, so it is shown only while that action is still the
// row's latest — taking the next action on the row, in any of its cells, removes an old error.
const latest = new Map<string, symbol>();
const listeners = new Set<() => void>();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => { listeners.delete(listener); };
}

export function useRowAction(row: string) {
  const mine = useRef(Symbol(row));
  const current = useSyncExternalStore(subscribe, () => latest.get(row));
  return {
    /** Marks this cell's action as the row's latest; call it as the action is taken. */
    take: () => {
      latest.set(row, mine.current);
      listeners.forEach((l) => l());
    },
    /** Whether this cell's action is the row's latest, so its error may show. */
    isLatest: current === mine.current,
  };
}
