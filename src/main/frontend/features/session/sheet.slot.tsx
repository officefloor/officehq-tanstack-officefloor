import { useState } from 'react';
import type { FormEvent } from 'react';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AppSession } from '../../slots/defs/appSession';
import { loadSheet, sheetKey, useCurrentSheet, useCurrentUser } from './session';

function SheetPicker() {
  const [user] = useCurrentUser();
  const [sheet, setSheet] = useCurrentSheet();
  const [id, setId] = useState('');
  const [error, setError] = useState('');
  // While a load re-reads the sheet, the current sheet is not shown, so nothing reads the screen as
  // loaded until it shows what the store holds now.
  const [loading, setLoading] = useState(false);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const load = useMutation({
    mutationFn: (sheetId: string) => loadSheet(sheetId),
    onSuccess: async (loaded) => {
      queryClient.setQueryData(sheetKey(loaded.id), loaded);
      // Re-loading a sheet re-reads everything kept under it from the store.
      await queryClient.invalidateQueries({
        predicate: (q) => q.queryKey[0] === 'sheet' && q.queryKey[1] === loaded.id && q.queryKey.length > 2,
      });
      setSheet(loaded.id);
    },
    onError: () => setError('The sheet could not be loaded.'),
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
    once(() => {
      setLoading(true);
      return load.mutateAsync(trimmed).finally(() => setLoading(false));
    });
  };

  return (
    <form data-testid="sheet-form" onSubmit={submit}>
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
      {error && <span data-testid="sheet-load-error">{error}</span>}
      {sheet && !loading && (
        <span>
          Sheet <strong data-testid="sheet-current-id">{sheet}</strong>
        </span>
      )}
    </form>
  );
}

export const contribution = AppSession.fill({ order: 10, Component: SheetPicker });
