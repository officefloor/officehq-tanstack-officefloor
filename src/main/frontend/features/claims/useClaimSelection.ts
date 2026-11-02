import { useSyncExternalStore } from 'react';

// The claims ticked for a bulk action, per sheet. Shared by every person's claim list (which ticks
// them) and the bulk bar (which acts on them), so it lives outside any one component.
const selected = new Map<string, ReadonlySet<number>>();
const listeners = new Set<() => void>();
const NONE: ReadonlySet<number> = new Set();

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function set(sheetId: string, ids: ReadonlySet<number>) {
  selected.set(sheetId, ids);
  listeners.forEach((l) => l());
}

/** The sheet's selected claim ids, a way to tick or untick one, and a way to clear them all. */
export function useClaimSelection(sheetId: string) {
  const ids = useSyncExternalStore(subscribe, () => selected.get(sheetId) ?? NONE);
  const toggle = (id: number, on: boolean) => {
    const next = new Set(selected.get(sheetId) ?? NONE);
    if (on) next.add(id);
    else next.delete(id);
    set(sheetId, next);
  };
  const clear = () => set(sheetId, NONE);
  return { ids, toggle, clear };
}
