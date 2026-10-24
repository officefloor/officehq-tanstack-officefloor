import { useRef } from 'react';
import type { UseMutationResult } from '@tanstack/react-query';

// Submits a mutation at most once at a time: a second submit (a double click) before the first has
// answered is dropped, so it stores one record, not two. `isPending` alone is not enough: it only
// disables the button once React has re-rendered, which a fast second click can beat.
export function useSubmitOnce<D, V, C>(mutation: UseMutationResult<D, Error, V, C>) {
  const busy = useRef(false);
  return (variables: V) => {
    if (busy.current) return;
    busy.current = true;
    mutation.reset();
    mutation.mutateAsync(variables).catch(() => undefined).finally(() => { busy.current = false; });
  };
}
