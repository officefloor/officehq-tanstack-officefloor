import { useCallback, useRef } from 'react';

/**
 * Runs a submit only when the one before it has answered, so a double click (two submits before
 * the button re-renders disabled) stores one record, not two. Pass a mutation's `mutateAsync`
 * call; its own onError shows any refusal.
 */
export function useOneAtATime() {
  const busy = useRef(false);
  return useCallback((start: () => Promise<unknown>) => {
    if (busy.current) return;
    busy.current = true;
    start()
      .catch(() => undefined)
      .finally(() => {
        busy.current = false;
      });
  }, []);
}
