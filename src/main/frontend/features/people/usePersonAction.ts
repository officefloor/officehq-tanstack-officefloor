import { useEffect, useRef } from 'react';

// The person row's actions (statement, retire, hold) share the row's one error: taking any of them
// clears the error another left showing, so only the latest action's refusal is shown.
const clearers = new Map<string, Set<() => void>>();

/** Registers this action's error clearing for the person; returns what to call as the action is taken. */
export function usePersonAction(personId: string, clear: () => void): () => void {
  const own = useRef(clear);
  own.current = clear;
  useEffect(() => {
    const fn = () => own.current();
    const set = clearers.get(personId) ?? new Set<() => void>();
    clearers.set(personId, set);
    set.add(fn);
    return () => {
      set.delete(fn);
    };
  }, [personId]);
  return () => clearers.get(personId)?.forEach((fn) => fn());
}
