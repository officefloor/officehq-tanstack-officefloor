import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { byUser } from '../session/session';
import { useSettings } from '../settings/settings';
import { sheetKey, useSheet } from './sheet';
import type { Sheet } from './sheet';

// The sheet's today: shown, and changeable. It starts as the real date. A steward archives the sheet here too;
// and clones it to another, never used, id; all share the sheet's error, and each action clears the others'. The sheet's parent (from its settings) shows here.
function SheetToday({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data } = useSheet(sheet);
  const { data: settings } = useSettings(sheet);
  const [draft, setDraft] = useState('');
  const save = useMutation({
    mutationFn: (today: string) =>
      postJson<Sheet>(byUser(`/api/sheets/${encodeURIComponent(sheet)}/today`, user), { today }),
    onSuccess: (updated) => {
      queryClient.setQueryData(sheetKey(sheet), updated);
      // Everything on the sheet may depend on its today: re-read it all.
      void queryClient.invalidateQueries({
        predicate: (q) => q.queryKey.includes(sheet) && q.queryKey[0] !== 'sheet',
      });
    },
  });
  const send = useSubmitOnce(save);
  const archive = useMutation({
    mutationFn: () => postJson<Sheet>(byUser(`/api/sheets/${encodeURIComponent(sheet)}/archive`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(sheetKey(sheet), updated),
  });

  const [cloneTo, setCloneTo] = useState('');
  const clone = useMutation({
    mutationFn: (to: string) =>
      postJson<{ to: string }>(byUser(`/api/sheets/${encodeURIComponent(sheet)}/clone`, user), { to }),
  });
  const resetAll = () => {
    save.reset();
    archive.reset();
    clone.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    archive.reset();
    clone.reset();
    send(draft.trim());
  };

  return (
    <form onSubmit={submit}>
      Today: <strong data-testid="sheet-today">{data?.today ?? ''}</strong>{' '}
      <input
        data-testid="sheet-today-input"
        type="text"
        placeholder="yyyy-mm-dd"
        value={draft}
        onChange={(e) => {
          setDraft(e.target.value);
          resetAll();
        }}
      />
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set today
      </button>
      {' '}Parent: <strong data-testid="sheet-parent">{settings?.parent ?? ''}</strong>{' '}
      {' '}Archived: <strong data-testid="sheet-archived">{data ? (data.archived ? 'yes' : 'no') : ''}</strong>{' '}
      <button
        type="button"
        data-testid="sheet-archive"
        disabled={archive.isPending}
        onClick={() => {
          resetAll();
          archive.mutate();
        }}
      >
        Archive sheet
      </button>{' '}
      <input
        data-testid="sheet-clone-to"
        type="text"
        placeholder="clone to sheet id"
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
        Clone sheet
      </button>
      {' '}Cloned to: <strong data-testid="sheet-cloned-to">{clone.data?.to ?? ''}</strong>
      {(save.isError || archive.isError || clone.isError) && (
        <span data-testid="sheet-error" role="alert">
          {' '}
          {clone.isError
            ? 'The sheet could not be cloned there.'
            : archive.isError
              ? 'The sheet could not be archived.'
              : 'That date was refused.'}
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 0, Component: SheetToday });
