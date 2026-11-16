import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSheet, type Sheet } from '../session/session';

/** The sheet's today: shown, and changeable. Changing it re-reads the whole sheet. Also whether the sheet is
 *  archived, and archiving it (a steward only), and cloning it to another, never used, id (a steward only), showing the
 *  last id it was cloned to. All share the sheet's one error element. */
function SheetToday({ sheetId, user }: { sheetId: string; user: string }) {
  const sheet = useSheet(sheetId);
  const queryClient = useQueryClient();
  const [typed, setTyped] = useState('');
  const [cloneTo, setCloneTo] = useState('');
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
  const clone = useMutation({
    mutationFn: (to: string) =>
      postJson<{ from: string; to: string }>(`/api/sheets/${encodeURIComponent(sheetId)}/clone`, { to, by: user }),
  });
  const resetAll = () => {
    save.reset();
    archive.reset();
    clone.reset();
  };
  return (
    <form
      data-testid="sheet-today-form"
      onSubmit={(e) => {
        e.preventDefault();
        if (!submit.begin()) return;
        archive.reset();
        clone.reset();
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
          resetAll();
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
          resetAll();
          archive.mutate();
        }}
      >
        Archive
      </button>{' '}
      <input
        data-testid="sheet-clone-to"
        placeholder="Clone to sheet id"
        value={cloneTo}
        onChange={(e) => {
          setCloneTo(e.target.value);
          resetAll();
        }}
      />
      <button
        type="button"
        data-testid="sheet-clone"
        disabled={clone.isPending}
        onClick={() => {
          resetAll();
          clone.mutate(cloneTo.trim());
        }}
      >
        Clone
      </button>{' '}
      Cloned to <span data-testid="sheet-cloned-to">{clone.data?.to ?? ''}</span>
      {(save.isError || archive.isError || clone.isError) && (
        <span data-testid="sheet-error">
          {clone.isError
            ? 'Could not clone the sheet.'
            : archive.isError
              ? 'Could not archive the sheet.'
              : 'Could not change today.'}
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPage.fill({ order: 0, Component: SheetToday });
