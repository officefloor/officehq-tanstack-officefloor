import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { AppSession } from '../../slots/defs/appSession';
import { useCurrentSheet, useCurrentUser } from './session';

// Choose a sheet by typing its id and loading it. Loading (again) re-reads everything kept for it.
function SheetPicker() {
  const [user] = useCurrentUser();
  const [sheet, setSheet] = useCurrentSheet();
  const [id, setId] = useState('');
  const queryClient = useQueryClient();
  const load = useMutation({
    mutationFn: (sheetId: string) => putJson<{ id: string }>(`/api/sheets/${encodeURIComponent(sheetId)}`),
    onSuccess: async (loaded) => {
      await queryClient.invalidateQueries({ queryKey: ['sheet', loaded.id] });
      setSheet(loaded.id);
    },
  });
  const submit = useSubmitOnce(load);

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
      {sheet && (
        <span>
          Sheet <strong data-testid="sheet-current-id">{sheet}</strong>
        </span>
      )}
      {load.isError && <span data-testid="sheet-load-error">Could not load the sheet.</span>}
    </form>
  );
}

export const contribution = AppSession.fill({ order: 10, Component: SheetPicker });
