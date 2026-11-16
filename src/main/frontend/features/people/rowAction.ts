import { useCallback, useSyncExternalStore } from 'react';

// The action last taken on each person's row (by sheet and person id). A row shows one error at a time: an action's
// error shows only while it is the latest action taken on that row, so taking the next action clears the last error.
const latest = new Map<string, string>();
const listeners = new Set<() => void>();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** Marks `action` as the latest taken on the person's row (call when it is taken), and whether it still is. */
export function usePersonRowAction(sheetId: string, personId: number, action: string) {
  const key = `${sheetId}\u0000${personId}`;
  const current = useSyncExternalStore(subscribe, () => latest.get(key));
  const take = useCallback(() => {
    latest.set(key, action);
    listeners.forEach((l) => l());
  }, [key, action]);
  return { take, isLatest: current === undefined || current === action };
}
