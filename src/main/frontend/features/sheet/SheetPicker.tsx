import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSheetId } from '../session/useSession';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';

// Choose a sheet by typing its id and loading it. Loading drops everything cached for that sheet,
// so the screen re-reads it from the store (an id never used before starts empty).
export function SheetPicker() {
  const [sheetId, setSheetId] = useSheetId();
  const [input, setInput] = useState('');
  const queryClient = useQueryClient();
  const load = useMutation({
    mutationFn: async (id: string) => {
      if (id.trim() === '') throw new Error('Enter a sheet id');
      return putJson<{ id: string }>(`/api/sheets/${encodeURIComponent(id.trim())}`);
    },
    onSuccess: (sheet) => {
      queryClient.removeQueries({ queryKey: ['sheet', sheet.id] });
      setSheetId(sheet.id);
    },
  });

  const submit = useSubmitOnce(load);
  return (
    <div data-testid="sheet-bar">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit(input);
        }}
      >
        <label>
          Sheet{' '}
          <input
            data-testid="sheet-id-input"
            value={input}
            onChange={(e) => {
              setInput(e.target.value);
              load.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="sheet-load" disabled={load.isPending}>
          Load
        </button>
      </form>
      <p>
        Active sheet: <strong data-testid="sheet-current-id">{sheetId}</strong>
      </p>
      {load.isError && (
        <p data-testid="sheet-load-error" role="alert">
          {load.error.message}
        </p>
      )}
    </div>
  );
}
