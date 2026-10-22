import { useEffect, useState } from 'react';

// Every action on a person's row shows its refusal in the row's one person-error; taking any action on the
// row clears the error an earlier one left, so a row never shows a stale refusal next to a new one.
const clearers = new Map<string, Set<() => void>>();

/** This cell's error on the person's row, its setter, and `begin`: call it as an action on the row is taken
 *  to clear every error the row's cells show. */
export function usePersonError(sheetId: string, personId: number | string) {
  const key = `${sheetId}/${personId}`;
  const [error, setError] = useState('');
  useEffect(() => {
    const clear = () => setError('');
    const set = clearers.get(key) ?? new Set();
    set.add(clear);
    clearers.set(key, set);
    return () => {
      set.delete(clear);
      if (set.size === 0) clearers.delete(key);
    };
  }, [key]);
  const begin = () => clearers.get(key)?.forEach((clear) => clear());
  return { error, setError, begin };
}
