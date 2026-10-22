import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useSheetId } from '../session/session';

import type { Sheet } from './sheet';

/** Choose a sheet by id. Loading an id never used before starts an empty sheet; loading an id
 *  again re-reads everything under it from the store. */
export function SheetPicker({ user }: { user: string }) {
  const queryClient = useQueryClient();
  const [sheetId, setSheetId] = useSheetId();
  const [typed, setTyped] = useState('');
  const once = useSubmitOnce();

  const load = useMutation({
    mutationFn: (id: string) => putJson<Sheet>(`/api/sheets/${encodeURIComponent(id)}`, { user }),
    onSuccess: async (sheet) => {
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheet.id] });
      setSheetId(sheet.id);
    },
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    load.reset();
    const id = typed.trim();
    if (id === '') {
      setSheetId(undefined);
      return;
    }
    if (!once.start()) return;
    load.mutate(id);
  };

  return (
    <section>
      <form data-testid="sheet-form" onSubmit={submit}>
        <label>
          Sheet id{' '}
          <input
            data-testid="sheet-id-input"
            value={typed}
            onChange={(e) => {
              setTyped(e.target.value);
              load.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="sheet-load" disabled={load.isPending}>
          Load
        </button>
        {load.isError && (
          <p data-testid="sheet-load-error" role="alert">
            Could not load that sheet.
          </p>
        )}
      </form>
      {sheetId && (
        <p>
          Sheet <strong data-testid="sheet-current-id">{sheetId}</strong>
        </p>
      )}
    </section>
  );
}
