import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { sheetKey, useSheet } from './sheet';
import type { Sheet } from './sheet';

// The sheet's today: shown, and changeable. A change re-reads everything kept for the sheet, as
// whatever depends on the date may now read differently. Also whether the sheet is archived, and
// the button a steward archives it with; once archived, every change on the sheet is refused.
function SheetToday({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const sheet = useSheet(sheetId);
  const [input, setInput] = useState('');
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const save = useMutation({
    mutationFn: (today: string) =>
      postJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}/today`, { today }, user),
    onSuccess: (updated) => {
      queryClient.setQueryData(sheetKey(sheetId), updated);
      void queryClient.invalidateQueries({
        queryKey: ['sheet', sheetId],
        predicate: (q) => q.queryKey[2] !== 'info',
      });
    },
    onError: () => setError('That date could not be set as today.'),
  });

  const archive = useMutation({
    mutationFn: () => postJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}/archive`, {}, user),
    onSuccess: (updated) => queryClient.setQueryData(sheetKey(sheetId), updated),
    onError: () => setError('The sheet could not be archived.'),
  });

  const archiveSheet = () => {
    setError('');
    once((done) => archive.mutate(undefined, { onSettled: done }));
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const today = input.trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(today)) {
      setError('Choose a date for today.');
      return;
    }
    once((done) => save.mutate(today, { onSettled: done }));
  };

  return (
    <form onSubmit={submit} data-testid="sheet-today-form">
      Today: <strong data-testid="sheet-today">{sheet.data?.today ?? ''}</strong>{' '}
      <label>
        Set today{' '}
        <input
          type="date"
          data-testid="sheet-today-input"
          value={input}
          onChange={(e) => {
            setInput(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set
      </button>{' '}
      Archived: <strong data-testid="sheet-archived">{sheet.data ? (sheet.data.archived ? 'yes' : 'no') : ''}</strong>{' '}
      <button type="button" data-testid="sheet-archive" onClick={archiveSheet} disabled={archive.isPending}>
        Archive sheet
      </button>
      {(error || sheet.isError) && (
        <p role="alert" data-testid="sheet-error">
          {error || 'The sheet could not be read.'}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: -100, Component: SheetToday });
