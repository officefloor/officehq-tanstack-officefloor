import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSheetId } from './session';

type Sheet = { id: string };

// Choose a sheet by typing its id and loading it. An id never used before starts an empty sheet;
// loading an id again drops what was cached for it, so the sheet is re-read from the store.
export function SheetLoader({ onLoaded }: { onLoaded: () => void }) {
  const queryClient = useQueryClient();
  const [sheetId, setSheetId] = useSheetId();
  const [input, setInput] = useState('');
  const [error, setError] = useState('');

  const load = useMutation({
    mutationFn: (id: string) => postJson<Sheet>(`/api/sheets/${encodeURIComponent(id)}/load`, {}),
    onSuccess: (sheet) => {
      queryClient.removeQueries({ queryKey: ['sheet', sheet.id] });
      setSheetId(sheet.id);
      onLoaded();
    },
    onError: () => setError('That sheet could not be loaded.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const id = input.trim();
    if (id === '') {
      setError('Type a sheet id to load.');
      return;
    }
    load.mutate(id);
  };

  return (
    <form onSubmit={submit} data-testid="sheet-form">
      <label>
        Sheet{' '}
        <input
          data-testid="sheet-id-input"
          value={input}
          onChange={(e) => {
            setInput(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="sheet-load" disabled={load.isPending}>
        Load
      </button>{' '}
      {sheetId && (
        <span>
          Active sheet: <strong data-testid="sheet-current-id">{sheetId}</strong>
        </span>
      )}
      {error && (
        <p role="alert" data-testid="sheet-load-error">
          {error}
        </p>
      )}
    </form>
  );
}
