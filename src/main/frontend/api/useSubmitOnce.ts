import { useRef } from 'react';

/**
 * Lets a form send one submit at a time. `isPending` (and the disabled button) only catch up on
 * the next render, so a double click can submit twice before then; this guard is set at once.
 * `start()` is false while a submit is still answering; call `done()` once it has answered.
 */
export function useSubmitOnce() {
  const busy = useRef(false);
  return {
    start: () => {
      if (busy.current) return false;
      busy.current = true;
      return true;
    },
    done: () => {
      busy.current = false;
    },
  };
}
