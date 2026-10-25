import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** A holiday of the sheet: its date (yyyy-mm-dd). */
export type Holiday = { date: string };

/** The key every feature reads the sheet's holidays under. */
export const holidaysKey = (sheetId: string) => ['sheet', sheetId, 'holidays'] as const;

/** The sheet's holidays, by date. */
export function useHolidays(sheetId: string) {
  return useQuery({
    queryKey: holidaysKey(sheetId),
    queryFn: () => getJson<Holiday[]>(`/api/sheets/${encodeURIComponent(sheetId)}/holidays`),
  });
}

// The holiday list: a date entry adds a holiday; a date already on the list is refused. A claim's due-by, fixed when
// it is added, is moved past the holidays on the sheet then; a holiday added later leaves existing due-bys as they are.
function HolidaysPanel({ sheetId }: { sheetId: string; user: string }) {
  const holidays = useHolidays(sheetId);
  const [date, setDate] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: async (value: string) => {
      if (value.trim() === '') throw new Error('Enter a holiday date');
      if (!DATE.test(value.trim())) throw new Error('Enter a date as YYYY-MM-DD');
      // `by` is the signed-in user the change is recorded against.
      return postJson<Holiday>(`/api/sheets/${encodeURIComponent(sheetId)}/holidays`, { date: value.trim(), by: user ?? '' });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: holidaysKey(sheetId) });
      setDate('');
    },
  });

  const submit = useSubmitOnce(add);
  return (
    <section data-testid="holidays-panel">
      <h2>Holidays</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit(date);
        }}
      >
        <input
          type="date"
          data-testid="holiday-date"
          value={date}
          onChange={(e) => {
            setDate(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="holiday-add" disabled={add.isPending}>
          Add holiday
        </button>
        {add.isError && (
          <span data-testid="holiday-error" role="alert">
            {' '}
            {add.error.message}
          </span>
        )}
      </form>
      <ul data-testid="holiday-list">
        {(holidays.data ?? []).map((h) => (
          <li key={h.date} data-testid={`holiday-row-${h.date}`}>
            {h.date}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 8, Component: HolidaysPanel });
