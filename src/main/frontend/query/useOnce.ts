import { useRef } from 'react';

// A form submitted twice before its first submit has answered (a double click) must store one
// record, not two. A mutation's isPending reaches the button only after a re-render, too late for
// the second click, so a submit is guarded by a ref that is set at once: `once(done => ...)` runs
// the start only when nothing is in flight, and the start calls `done` when its request settles.
export function useOnce() {
  const busy = useRef(false);
  return (start: (done: () => void) => void) => {
    if (busy.current) return;
    busy.current = true;
    start(() => {
      busy.current = false;
    });
  };
}
