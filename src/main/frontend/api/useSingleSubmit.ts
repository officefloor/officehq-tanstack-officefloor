import { useRef } from 'react';

// A form submitted again before its first submit has answered sends nothing more: a double click on
// submit stores one record, not two. The `disabled={isPending}` on a submit button is not enough on
// its own, as a second click can land before the button re-renders disabled.
export function useSingleSubmit(): (submit: () => Promise<unknown>) => void {
  const busy = useRef(false);
  return (submit) => {
    if (busy.current) return;
    busy.current = true;
    // A refusal is shown through the mutation's error state; nothing more to do with it here.
    submit()
      .catch(() => undefined)
      .finally(() => {
        busy.current = false;
      });
  };
}
