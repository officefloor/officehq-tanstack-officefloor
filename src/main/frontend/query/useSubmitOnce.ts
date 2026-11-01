import { useRef } from 'react';
import type { UseMutationResult } from '@tanstack/react-query';

// Sends a mutation unless one sent this way has not answered yet, so a form submitted twice in quick
// succession (a double click) stores one record. `isPending` only shows on the next render, too late
// for the second click; this guard is a ref, set before the first send returns.
export function useSubmitOnce<V>(mutation: UseMutationResult<unknown, unknown, V, unknown>) {
  const busy = useRef(false);
  return (vars: V) => {
    if (busy.current) return;
    busy.current = true;
    mutation.reset();
    mutation
      .mutateAsync(vars)
      .catch(() => {
        /* a refusal shows through the mutation's own error state */
      })
      .finally(() => {
        busy.current = false;
      });
  };
}
