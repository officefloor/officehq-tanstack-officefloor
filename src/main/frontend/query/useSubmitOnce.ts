import { useRef } from 'react';

/** Guards a form's submit so a second submit made before the first has answered (a double click)
 *  is ignored: the store gets one record, not two. A ref, not mutation state, so it holds even
 *  for two clicks landing before the next render. */
export function useSubmitOnce(): (run: () => Promise<unknown>) => void {
  const busy = useRef(false);
  return (run) => {
    if (busy.current) return;
    busy.current = true;
    run()
      .catch(() => {
        // the mutation shows its own error
      })
      .finally(() => {
        busy.current = false;
      });
  };
}
