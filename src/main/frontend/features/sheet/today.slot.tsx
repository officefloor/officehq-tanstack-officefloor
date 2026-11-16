import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSheet, type Sheet } from '../session/session';

/** The sheet's today: shown, and changeable. Changing it re-reads the whole sheet. Also whether the sheet is
 *  archived, and archiving it (a steward only). Both share the sheet's one error element. */
function SheetToday({ sheetId, user }: { sheetId: string; user: string }) {
  const sheet = useSheet(sheetId);
  const queryClient = useQueryClient();
  const [typed, setTyped] = useState('');
  const submit = useSubmitOnce();
  const stored = (saved: Sheet) => {
    queryClient.setQueryData(['sheet', sheetId], saved);
    void queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
  };
  const save = useMutation({
    mutationFn: (today: string) =>
      putJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}/today`, { today, by: user }),
    onSuccess: stored,
    onSettled: submit.end,
  });
  const archive = useMutation({
    mutationFn: () => postJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}/archive`, { by: user }),
    onSuccess: stored,
  });
  return (
    <form
      data-testid="sheet-today-form"
      onSubmit={(e) => {
        e.preventDefault();
        if (!submit.begin()) return;
        archive.reset();
        save.mutate(typed.trim());
      }}
    >
      Today <span data-testid="sheet-today">{sheet.data?.today}</span>{' '}
      <input
        type="date"
        data-testid="sheet-today-input"
        value={typed}
        onChange={(e) => {
          setTyped(e.target.value);
          save.reset();
          archive.reset();
        }}
      />
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set today
      </button>{' '}
      Parent <span data-testid="sheet-parent">{sheet.data?.parent ?? ''}</span>{' '}
      Archived <span data-testid="sheet-archived">{sheet.data?.archived ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="sheet-archive"
        disabled={archive.isPending}
        onClick={() => {
          save.reset();
          archive.reset();
          archive.mutate();
        }}
      >
        Archive
      </button>
      {(save.isError || archive.isError) && (
        <span data-testid="sheet-error">
          {archive.isError ? 'Could not archive the sheet.' : 'Could not change today.'}
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPage.fill({ order: 0, Component: SheetToday });
