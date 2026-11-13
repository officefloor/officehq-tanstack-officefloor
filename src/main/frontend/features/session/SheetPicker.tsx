import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { sheetKey, useCurrentSheet, useCurrentUser } from './session';

// Choose a sheet by typing its id and loading it. Loading an id never used before starts an empty
// sheet; loading one again re-reads what the store holds for it now.
export function SheetPicker() {
  const [user] = useCurrentUser();
  const [sheet, setSheet] = useCurrentSheet();
  const [id, setId] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();

  const load = useMutation({
    mutationFn: (sheetId: string) => postJson<{ id: string }>('/api/sheets', { id: sheetId }),
    onSuccess: async ({ id: loaded }) => {
      setSheet(loaded);
      await queryClient.invalidateQueries({ queryKey: sheetKey(loaded) });
    },
    onError: () => setError('That sheet could not be loaded.'),
  });

  if (!user) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = id.trim();
    setError('');
    if (!trimmed) {
      setError('Type a sheet id to load.');
      return;
    }
    load.mutate(trimmed);
  };

  return (
    <form onSubmit={submit}>
      <span>
        Sheet: <strong data-testid="sheet-current-id">{sheet}</strong>{' '}
      </span>
      <input
        data-testid="sheet-id-input"
        placeholder="Sheet id"
        value={id}
        onChange={(e) => {
          setId(e.target.value);
          setError('');
        }}
      />
      <button type="submit" data-testid="sheet-load" disabled={load.isPending}>
        Load
      </button>
      {error && <span data-testid="sheet-error">{error}</span>}
    </form>
  );
}
