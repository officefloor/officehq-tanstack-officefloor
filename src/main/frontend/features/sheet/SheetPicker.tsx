import { useState } from 'react';
import { type QueryClient, useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useActiveSheet } from '../session/useSession';

type Sheet = { id: string };

/** Resolve once the screen has read the sheet: its queries have mounted (after the next frame) and
 *  none of them is still fetching. */
function sheetRead(queryClient: QueryClient, id: string): Promise<void> {
  const fetching = () => queryClient.isFetching({ queryKey: ['sheet', id] }) > 0;
  return new Promise((resolve) => {
    requestAnimationFrame(() =>
      setTimeout(() => {
        if (!fetching()) return resolve();
        const stop = queryClient.getQueryCache().subscribe(() => {
          if (!fetching()) {
            stop();
            resolve();
          }
        });
      }, 0),
    );
  });
}

/** Choose a sheet by typing its id and loading it. Loading (even the same id again) drops every
 *  ['sheet', id, ...] query, so the sheet is re-read from the store. The active id is shown once
 *  the screen has read the sheet, so a shown id means its rows are on screen. */
export function SheetPicker() {
  const [active, setActive] = useActiveSheet();
  const [id, setId] = useState(active);
  const [shown, setShown] = useState(active);
  const queryClient = useQueryClient();
  const load = useMutation({
    mutationFn: async (raw: string) => {
      const want = raw.trim();
      if (want === '') throw new Error('sheet id is required');
      return postJson<Sheet>(`/api/sheets/${encodeURIComponent(want)}`, {});
    },
    onSuccess: async (sheet) => {
      queryClient.removeQueries({ queryKey: ['sheet', sheet.id] });
      setShown('');
      setActive(sheet.id);
      await sheetRead(queryClient, sheet.id);
      setShown(sheet.id);
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
      Active sheet: <strong data-testid="sheet-current-id">{shown}</strong>
      {load.isError && <p data-testid="sheet-error">{load.error.message}</p>}
    </form>
  );
}
