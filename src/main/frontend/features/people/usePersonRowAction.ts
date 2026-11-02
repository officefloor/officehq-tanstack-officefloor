import { useEffect, useId } from 'react';

// The actions on a person's row (statement, hold, retire) live in separate slots, but share the row's one
// `person-error`. Taking any of them announces it on the row, and every other action there clears its old
// error, so a refusal left by one action never shows beside (or for) the next.
const bus = new EventTarget();

/** Clear this action's error (`reset`) when another action on the same row is taken; returns the
 *  `announce` to call as this action is taken. */
export function usePersonRowAction(sheetId: string, personId: number, reset: () => void): () => void {
  const self = useId();
  const row = `${sheetId}/${personId}`;
  useEffect(() => {
    const listener = (e: Event) => {
      const d = (e as CustomEvent<{ row: string; from: string }>).detail;
      if (d.row === row && d.from !== self) reset();
    };
    bus.addEventListener('person-action', listener);
    return () => bus.removeEventListener('person-action', listener);
  }, [row, self, reset]);
  return () => bus.dispatchEvent(new CustomEvent('person-action', { detail: { row, from: self } }));
}
