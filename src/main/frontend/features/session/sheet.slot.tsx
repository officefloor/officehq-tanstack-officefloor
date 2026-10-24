import { useCallback, useState, useSyncExternalStore } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { AppSession } from '../../slots/defs/appSession';
import { useCurrentSheet, useCurrentUser } from './session';

// Whether every query kept for the sheet has been read at least once since it was (re-)loaded — so the
// sheet's id shows only once the screen holds what the store holds for it, not the empty or old data
// before. A refetch that keeps the data it had (after a change) does not hide it.
function useSheetRead(sheet: string | undefined): boolean {
  const queryClient = useQueryClient();
  const cache = queryClient.getQueryCache();
  const subscribe = useCallback((onChange: () => void) => cache.subscribe(onChange), [cache]);
  return useSyncExternalStore(subscribe, () => {
    if (!sheet) return false;
    const queries = cache.findAll({ queryKey: ['sheet', sheet] });
    return queries.length > 0 && queries.every((q) => q.state.status !== 'pending' || q.isDisabled());
  });
}

// Choose a sheet by typing its id and loading it. Loading (again) re-reads everything kept for it.
function SheetPicker() {
  const [user] = useCurrentUser();
  const [sheet, setSheet] = useCurrentSheet();
  const [id, setId] = useState('');
  const queryClient = useQueryClient();
  const load = useMutation({
    mutationFn: (sheetId: string) => putJson<{ id: string }>(`/api/sheets/${encodeURIComponent(sheetId)}`),
    onSuccess: async (loaded) => {
      // Drop what was read for it before, so the screen shows the store's data, never an older copy.
      await queryClient.resetQueries({ queryKey: ['sheet', loaded.id] });
      setSheet(loaded.id);
    },
  });
  const submit = useSubmitOnce(load);
  const read = useSheetRead(sheet);

  if (!user) return null;
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (id.trim()) submit(id.trim());
      }}
    >
      <input data-testid="sheet-id-input" placeholder="Sheet id" value={id} onChange={(e) => { setId(e.target.value); load.reset(); }} />
      <button type="submit" data-testid="sheet-load" disabled={load.isPending}>
        Load sheet
      </button>
      {sheet && read && (
        <span>
          Sheet <strong data-testid="sheet-current-id">{sheet}</strong>
        </span>
      )}
      {load.isError && <span data-testid="sheet-load-error">Could not load the sheet.</span>}
    </form>
  );
}

export const contribution = AppSession.fill({ order: 10, Component: SheetPicker });
