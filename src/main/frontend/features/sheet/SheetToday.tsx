import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, putJson } from '../../api/http';
import { useSingleSubmit } from '../../api/useSingleSubmit';

// The sheet's "today" (yyyy-MM-dd). It starts as the real date and is changed here. Anything that
// depends on the date, or defaults a date, reads it with useSheetToday — never the browser clock.
type Today = { today: string };

const todayKey = (sheet: string) => ['sheet', sheet, 'today'];
const todayPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/today`;

export function useSheetToday(sheet: string): string | undefined {
  return useQuery({
    queryKey: todayKey(sheet),
    queryFn: () => getJson<Today>(todayPath(sheet)),
    enabled: !!sheet,
  }).data?.today;
}

export function SheetToday({ sheet }: { sheet: string }) {
  const today = useSheetToday(sheet);
  const queryClient = useQueryClient();
  const [date, setDate] = useState('');
  const submitOnce = useSingleSubmit();
  const save = useMutation({
    mutationFn: (value: string) => putJson<Today>(todayPath(sheet), { today: value }),
    onSuccess: (saved) => {
      queryClient.setQueryData(todayKey(sheet), saved);
      void queryClient.invalidateQueries({ queryKey: ['sheet', sheet] });
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          save.reset();
          return save.mutateAsync(date.trim());
        });
      }}
    >
      Today: <strong data-testid="sheet-today">{today}</strong>{' '}
      <label>
        Set today{' '}
        <input
          type="date"
          data-testid="sheet-today-input"
          value={date}
          onChange={(e) => {
            save.reset();
            setDate(e.target.value);
          }}
        />
      </label>
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Save
      </button>
      {save.isError && (
        <span role="alert" data-testid="sheet-today-error">
          {' '}Not a valid date.
        </span>
      )}
    </form>
  );
}
