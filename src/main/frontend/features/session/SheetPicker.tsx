import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useSheetId } from './session';

/** Choose the active sheet by typing its id. Loading an id again re-reads it from the store. */
export function SheetPicker() {
  const [sheetId, setSheetId] = useSheetId();
  const queryClient = useQueryClient();
  const [typed, setTyped] = useState('');
  return (
    <form
      data-testid="sheet-picker"
      onSubmit={(e) => {
        e.preventDefault();
        const id = typed.trim();
        if (id === '') return;
        void queryClient.invalidateQueries({ queryKey: ['sheet', id] });
        if (id !== sheetId) setSheetId(id);
      }}
    >
      <label>
        Sheet <input data-testid="sheet-id-input" value={typed} onChange={(e) => setTyped(e.target.value)} />
      </label>
      <button type="submit" data-testid="sheet-load">
        Load
      </button>
    </form>
  );
}
