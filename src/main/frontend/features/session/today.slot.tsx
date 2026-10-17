import { useState } from 'react';
import type { FormEvent } from 'react';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { AppSession } from '../../slots/defs/appSession';
import { sheetKey, useCurrentSheet, useSheetToday } from './session';
import type { Sheet } from './session';

// The active sheet's "today": shown and changeable here. It starts as the real date.
function SheetToday() {
  const [sheet] = useCurrentSheet();
  const today = useSheetToday();
  const [date, setDate] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (value: string) =>
      postJson<Sheet>(`/api/sheets/${encodeURIComponent(sheet!)}/today`, { today: value }),
    onSuccess: async (saved) => {
      queryClient.setQueryData(sheetKey(saved.id), saved);
      // Anything kept under the sheet may depend on its today.
      await queryClient.invalidateQueries({
        predicate: (q) => q.queryKey[0] === 'sheet' && q.queryKey[1] === saved.id && q.queryKey.length > 2,
      });
    },
    onError: () => setError('Today must be a date (yyyy-mm-dd).'),
  });

  if (!sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date.trim())) {
      setError('Today must be a date (yyyy-mm-dd).');
      return;
    }
    once(() => save.mutateAsync(date.trim()));
  };

  return (
    <form data-testid="sheet-today-form" onSubmit={submit}>
      <span>
        Today <strong data-testid="sheet-today">{today ?? ''}</strong>
      </span>
      <input
        type="date"
        data-testid="sheet-today-input"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          setError('');
        }}
      />
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set today
      </button>
      {error && <span data-testid="sheet-today-error">{error}</span>}
    </form>
  );
}

export const contribution = AppSession.fill({ order: 20, Component: SheetToday });
