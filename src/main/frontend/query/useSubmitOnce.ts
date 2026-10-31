import { useRef } from 'react';

/**
 * Guards a form's submit so a second submit before the first has answered is dropped: a double
 * click stores one record, not two. A `disabled={isPending}` button alone is not enough, as the
 * mutation's pending state reaches the button only after a re-render, while the latch is set at
 * once. Pass the mutation's `mutateAsync` call; its failure still shows through the mutation.
 */
export function useSubmitOnce() {
  const busy = useRef(false);
  return (run: () => Promise<unknown>) => {
    if (busy.current) return;
    busy.current = true;
    run()
      .catch(() => {
        /* shown through the mutation's error state */
      })
      .finally(() => {
        busy.current = false;
      });
  };
}
