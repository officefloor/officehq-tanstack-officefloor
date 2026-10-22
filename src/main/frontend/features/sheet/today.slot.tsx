import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { sheetKey, useSheet } from './sheet';
import type { Sheet } from './sheet';

/** The sheet's today: shown, and changeable. Changing it re-reads everything under the sheet. */
function SheetToday({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const sheet = useSheet(sheetId);
  const [typed, setTyped] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (today: string) =>
      putJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}/today`, { user, today }),
    onSuccess: async (saved) => {
      queryClient.setQueryData(sheetKey(sheetId), saved);
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!once.start()) return;
    save.reset();
    save.mutate(typed.trim());
  };

  return (
    <section>
      <p>
        Today <strong data-testid="sheet-today">{sheet.data?.today ?? ''}</strong>
      </p>
      <form data-testid="sheet-today-form" onSubmit={submit}>
        <label>
          Change today{' '}
          <input
            data-testid="sheet-today-input"
            placeholder="yyyy-mm-dd"
            value={typed}
            onChange={(e) => {
              setTyped(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
          Set today
        </button>
      </form>
      {save.isError && (
        <p data-testid="sheet-error" role="alert">
          Could not change today: enter a real date as yyyy-mm-dd.
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 0, Component: SheetToday });
