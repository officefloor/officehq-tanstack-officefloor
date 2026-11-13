import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { sheetInfoKey, sheetKey, useCurrentSheet, useCurrentUser } from './session';
import type { SheetInfo } from './session';
import { useSheetInfo, useSheetToday } from './sheetToday';
import { useOneAtATime } from '../../query/useOneAtATime';

// The sheet's own "today": shown, and changed by typing a date and saving. A blank date resets it
// to the real date. A steward can archive the sheet here: an archived sheet refuses every change.
export function SheetToday() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const today = useSheetToday();
  const info = useSheetInfo();
  const [date, setDate] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const save = useMutation({
    mutationFn: (value: string) =>
      putJson<SheetInfo>(`/api/sheets/${encodeURIComponent(sheet)}/today?user=${encodeURIComponent(user)}`, { today: value }),
    onSuccess: async (saved) => {
      queryClient.setQueryData(sheetInfoKey(saved.id), saved);
      // Everything that depends on the date is kept under the sheet's key.
      await queryClient.invalidateQueries({ queryKey: sheetKey(saved.id) });
    },
    onError: () => setError('That date could not be set as today.'),
  });

  const archive = useMutation({
    mutationFn: () =>
      postJson<SheetInfo>(`/api/sheets/${encodeURIComponent(sheet)}/archive?user=${encodeURIComponent(user)}`, {}),
    onSuccess: (saved) => queryClient.setQueryData(sheetInfoKey(saved.id), saved),
    onError: () => setError('The sheet could not be archived.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = date.trim();
    setError('');
    if (trimmed && !/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
      setError('Type a date as yyyy-mm-dd.');
      return;
    }
    once(() => save.mutateAsync(trimmed));
  };

  return (
    <form onSubmit={submit}>
      <span>
        Today: <strong data-testid="sheet-today">{today}</strong>{' '}
      </span>
      <input
        data-testid="sheet-today-input"
        placeholder="yyyy-mm-dd"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          setError('');
        }}
      />
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set today
      </button>
      <span>
        {' '}Archived: <strong data-testid="sheet-archived">{info.data ? (info.data.archived ? 'yes' : 'no') : ''}</strong>{' '}
      </span>
      <button
        type="button"
        data-testid="sheet-archive"
        disabled={archive.isPending}
        onClick={() => {
          setError('');
          once(() => archive.mutateAsync());
        }}
      >
        Archive sheet
      </button>
      {error && <span data-testid="sheet-error">{error}</span>}
    </form>
  );
}
