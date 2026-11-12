import { useRef } from 'react';
import type { UseMutationResult } from '@tanstack/react-query';

// A mutation's isPending reaches the screen only on a later render, so a double click submits a
// form twice before its button is disabled. This guard is set synchronously: a submit made while
// the form's previous one has not answered yet is dropped, so the form stores one record, not two.
export function useSubmitOnce<V>(mutation: UseMutationResult<unknown, Error, V, unknown>) {
  const busy = useRef(false);
  return (variables: V) => {
    if (busy.current) return;
    busy.current = true;
    mutation.reset();
    mutation
      .mutateAsync(variables)
      .catch(() => undefined) // the error shows through mutation.isError
      .finally(() => {
        busy.current = false;
      });
  };
}
