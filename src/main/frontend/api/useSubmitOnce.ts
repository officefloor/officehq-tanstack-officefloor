import { useRef } from 'react';

/**
 * Guard a form's submit so a second submit while the first is still unanswered (a double click)
 * sends nothing. The guard is a ref, set synchronously, so it holds before React re-renders the
 * button as disabled.
 *
 *     const once = useSubmitOnce();
 *     once(() => save.mutateAsync(body));
 */
export function useSubmitOnce() {
  const busy = useRef(false);
  return (run: () => Promise<unknown>) => {
    if (busy.current) return;
    busy.current = true;
    run()
      .catch(() => undefined) // the mutation's onError shows the refusal
      .finally(() => {
        busy.current = false;
      });
  };
}
