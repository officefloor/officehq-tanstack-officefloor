import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { useParent } from '../settings/settings';
import { sheetKey, useSheet } from './sheet';
import type { Sheet } from './sheet';

/** The sheet's today: shown, and changeable. Changing it re-reads everything under the sheet. Also whether the
 *  sheet is archived, and its parent sheet (nothing when none), and the button a steward archives it with; both share the sheet's one error. */
function SheetToday({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const sheet = useSheet(sheetId);
  const parent = useParent(sheetId);
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

  const archive = useMutation({
    mutationFn: () => postJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}/archive`, { user }),
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
    archive.reset();
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
              archive.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
          Set today
        </button>
      </form>
      <p>
        Parent <strong data-testid="sheet-parent">{parent}</strong>{' '}
        Archived <strong data-testid="sheet-archived">{sheet.data ? (sheet.data.archived ? 'yes' : 'no') : ''}</strong>{' '}
        <button
          type="button"
          data-testid="sheet-archive"
          disabled={archive.isPending}
          onClick={() => {
            if (!once.start()) return;
            save.reset();
            archive.reset();
            archive.mutate();
          }}
        >
          Archive sheet
        </button>
      </p>
      {(save.isError || archive.isError) && (
        <p data-testid="sheet-error" role="alert">
          {archive.isError
            ? archive.error instanceof HttpError && archive.error.body
              ? archive.error.body
              : 'Could not archive the sheet.'
            : save.error instanceof HttpError && save.error.status === 403 && save.error.body
              ? save.error.body
              : 'Could not change today: enter a real date as yyyy-mm-dd.'}
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 0, Component: SheetToday });
