import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useActiveSheet } from '../session/useSession';

type Sheet = { id: string };

/** Choose a sheet by typing its id and loading it. Loading (even the same id again) drops every
 *  ['sheet', id, ...] query, so the sheet is re-read from the store. */
export function SheetPicker() {
  const [active, setActive] = useActiveSheet();
  const [id, setId] = useState(active);
  const queryClient = useQueryClient();
  const load = useMutation({
    mutationFn: async (raw: string) => {
      const want = raw.trim();
      if (want === '') throw new Error('sheet id is required');
      return postJson<Sheet>(`/api/sheets/${encodeURIComponent(want)}`, {});
    },
    onSuccess: (sheet) => {
      queryClient.removeQueries({ queryKey: ['sheet', sheet.id] });
      setActive(sheet.id);
    },
  });

  return (
    <form
      data-testid="sheet-form"
      onSubmit={(e) => {
        e.preventDefault();
        load.reset();
        load.mutate(id);
      }}
    >
      <label>
        Sheet{' '}
        <input
          data-testid="sheet-id-input"
          value={id}
          onChange={(e) => {
            setId(e.target.value);
            load.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="sheet-load" disabled={load.isPending}>
        Load
      </button>{' '}
      Active sheet: <strong data-testid="sheet-current-id">{active}</strong>
      {load.isError && <p data-testid="sheet-error">{load.error.message}</p>}
    </form>
  );
}
