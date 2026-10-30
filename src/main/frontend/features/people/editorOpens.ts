import { useSyncExternalStore } from 'react';

// Counts presses of a person's edit button, so pressing it again for the person whose editor is
// already open (say after a refused save) refills the form with their stored values.
let opens = 0;
const listeners = new Set<() => void>();

export function openedEditor() {
  opens++;
  listeners.forEach((l) => l());
}

export function useEditorOpens(): number {
  return useSyncExternalStore(
    (l) => {
      listeners.add(l);
      return () => listeners.delete(l);
    },
    () => opens,
  );
}
