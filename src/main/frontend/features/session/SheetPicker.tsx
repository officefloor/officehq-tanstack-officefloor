import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { asString, useSearchParam } from '../../url/useSearchParam';

// Choose the active sheet by id. The active sheet is URL state (`sheet`); everything else is kept
// per sheet, under query keys starting ['sheet', id]. Loading an id (again) re-reads it from the store.
export function useSheetId(): string {
  return useSearchParam('sheet', asString)[0];
}

export function SheetPicker() {
  const [sheet, setSheet] = useSearchParam('sheet', asString);
  const [id, setId] = useState('');
  const queryClient = useQueryClient();

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const trimmed = id.trim();
        if (!trimmed) return;
        setSheet(trimmed);
        void queryClient.invalidateQueries({ queryKey: ['sheet', trimmed] });
      }}
    >
      <label>
        Sheet <input data-testid="sheet-id-input" value={id} onChange={(e) => setId(e.target.value)} />
      </label>
      <button type="submit" data-testid="sheet-load">
        Load
      </button>
      {sheet && (
        <span>
          {' '}Active sheet: <strong data-testid="sheet-current-id">{sheet}</strong>
        </span>
      )}
    </form>
  );
}
