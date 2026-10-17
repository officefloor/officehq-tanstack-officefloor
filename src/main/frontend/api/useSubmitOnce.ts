import { useCallback, useRef } from 'react';

// Guards a form against a second submit (a double click) while the first is still unanswered:
// `disabled={isPending}` only lands on the next render, too late for the second click. Pass the
// submit as a promise (e.g. `() => save.mutateAsync(input)`); its failure is left to the
// mutation's onError.
export function useSubmitOnce() {
  const pending = useRef(false);
  return useCallback((run: () => Promise<unknown>) => {
    if (pending.current) return;
    pending.current = true;
    run()
      .catch(() => undefined)
      .finally(() => {
        pending.current = false;
      });
  }, []);
}
