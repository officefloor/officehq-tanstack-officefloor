import { useCallback, useRef } from 'react';

/** Guards a form's submit so a second submit before the first has answered is dropped: a button's
 *  `disabled={isPending}` only applies after a re-render, too late for a double click. */
export function useSubmitOnce() {
  const busy = useRef(false);
  return useCallback((run: () => Promise<unknown>) => {
    if (busy.current) return;
    busy.current = true;
    run()
      .catch(() => undefined) // the mutation's own state shows the refusal
      .finally(() => {
        busy.current = false;
      });
  }, []);
}
