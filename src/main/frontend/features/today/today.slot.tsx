import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSheetToday, type SheetToday } from './useSheetToday';

/** The sheet's today, shown and changeable. Changing it re-reads the whole sheet, since every
 *  date decision in it depends on today. */
function TodayControl({ sheetId }: { sheetId: string }) {
  const today = useSheetToday(sheetId);
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (raw: string) =>
      postJson<SheetToday>(`/api/sheets/${encodeURIComponent(sheetId)}/today`, { date: raw.trim() }),
    onSuccess: (sheet) => {
      queryClient.setQueryData(['sheet', sheetId, 'today'], sheet);
      void queryClient.invalidateQueries({
        queryKey: ['sheet', sheetId],
        predicate: (q) => q.queryKey[2] !== 'today',
      });
    },
  });

  return (
    <form
      data-testid="sheet-today-form"
      onSubmit={(e) => {
        e.preventDefault();
        save.reset();
        save.mutate(date);
      }}
    >
      Today: <strong data-testid="sheet-today">{today.data?.today ?? ''}</strong>{' '}
      <label>
        Set today{' '}
        <input
          type="text"
          placeholder="yyyy-mm-dd"
          data-testid="sheet-today-input"
          value={date}
          onChange={(e) => {
            setDate(e.target.value);
            save.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set
      </button>
      {save.isError && <p data-testid="sheet-error">{save.error.message}</p>}
    </form>
  );
}

export const contribution = SheetBody.fill({
  order: 0,
  Component: ({ sheetId }) => <TodayControl sheetId={sheetId} />,
});
