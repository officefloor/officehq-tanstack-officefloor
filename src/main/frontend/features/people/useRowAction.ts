import { useEffect, useRef } from 'react';

// The actions of a person's row (statement, hold, retire) share the row's one error element; taking
// any of them clears the error another left showing, so only the latest action's error remains.
type Listener = { sheet: string; personId: number; owner: object; reset: () => void };
const listeners = new Set<Listener>();

/** Register a row action's `reset`; the returned `taken` is called as the action is taken. */
export function useRowAction(sheet: string, personId: number, reset: () => void): () => void {
  const owner = useRef({}).current;
  const resetRef = useRef(reset);
  resetRef.current = reset;
  useEffect(() => {
    const l: Listener = { sheet, personId, owner, reset: () => resetRef.current() };
    listeners.add(l);
    return () => {
      listeners.delete(l);
    };
  }, [sheet, personId, owner]);
  return () => {
    for (const l of listeners) {
      if (l.owner !== owner && l.sheet === sheet && l.personId === personId) l.reset();
    }
  };
}
