import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { sheetInfoKey, sheetKey, useCurrentSheet, useCurrentUser } from './session';
import type { SheetInfo } from './session';
import { useSheetToday } from './sheetToday';
import { useOneAtATime } from '../../query/useOneAtATime';

// The sheet's own "today": shown, and changed by typing a date and saving. A blank date resets it
// to the real date.
export function SheetToday() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const today = useSheetToday();
  const [date, setDate] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const save = useMutation({
    mutationFn: (value: string) =>
      putJson<SheetInfo>(`/api/sheets/${encodeURIComponent(sheet)}/today`, { today: value }),
    onSuccess: async (saved) => {
      queryClient.setQueryData(sheetInfoKey(saved.id), saved);
      // Everything that depends on the date is kept under the sheet's key.
      await queryClient.invalidateQueries({ queryKey: sheetKey(saved.id) });
    },
    onError: () => setError('That date could not be set as today.'),
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
      {error && <span data-testid="sheet-error">{error}</span>}
    </form>
  );
}
