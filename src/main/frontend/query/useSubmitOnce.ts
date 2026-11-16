import { useRef } from 'react';

/** Guards a form against a second submit before the first has answered (a double click): `begin`
 *  is true only when no submit is in flight, and `end` (call it when the mutation settles) frees it.
 *  A ref, not the mutation's isPending, which reaches the button only after a render. */
export function useSubmitOnce() {
  const busy = useRef(false);
  return {
    begin: () => {
      if (busy.current) return false;
      busy.current = true;
      return true;
    },
    end: () => {
      busy.current = false;
    },
  };
}
