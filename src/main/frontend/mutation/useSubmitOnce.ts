import { useRef } from 'react';
import type { UseMutationResult } from '@tanstack/react-query';

/**
 * Runs a form's mutation, ignoring a second submit while the first has not answered (a double
 * click), so one submit stores one record. The guard is a ref, set synchronously, because a
 * second click can land before `isPending` has re-rendered the button disabled.
 */
export function useSubmitOnce<V>(mutation: UseMutationResult<unknown, Error, V, unknown>) {
  const inFlight = useRef(false);
  return (vars: V) => {
    if (inFlight.current) return;
    inFlight.current = true;
    mutation
      .mutateAsync(vars)
      .catch(() => {
        // the refusal is shown from the mutation's own error state
      })
      .finally(() => {
        inFlight.current = false;
      });
  };
}
