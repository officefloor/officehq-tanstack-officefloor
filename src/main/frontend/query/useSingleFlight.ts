import { useRef } from 'react';

/** Run a submit at most once at a time: a second submit (a double click) made before the first
 *  has answered is dropped, so it stores one record, not two. The guard is a ref, set at once,
 *  so it holds before the pending state has re-rendered a disabled button. */
export function useSingleFlight() {
  const busy = useRef(false);
  return (start: () => Promise<unknown>) => {
    if (busy.current) return;
    busy.current = true;
    start()
      .catch(() => {
        /* the mutation shows its own error */
      })
      .finally(() => {
        busy.current = false;
      });
  };
}
