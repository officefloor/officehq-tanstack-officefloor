import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { holidaysKey, useHolidays, type Holiday } from './holidays';

/** The sheet's holidays: the date entry and button that add one, and each holiday on the list. A date already on the
 *  list is refused. A claim's due-by falling on a holiday as it is added moves to the next day that is not one. */
function Holidays({ sheetId, user }: { sheetId: string; user: string }) {
  const holidays = useHolidays(sheetId);
  const queryClient = useQueryClient();
  const [date, setDate] = useState('');

  const submit = useSubmitOnce();
  const add = useMutation({
    mutationFn: (d: string) => postJson<Holiday>(`/api/sheets/${encodeURIComponent(sheetId)}/holidays`, { date: d, by: user }),
    onSuccess: async () => {
      setDate('');
      await queryClient.invalidateQueries({ queryKey: holidaysKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <section data-testid="holidays">
      <h3>Holidays</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          add.reset();
          add.mutate(date);
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
        {add.isError && <span data-testid="holiday-error"> A holiday needs a date not already on the list.</span>}
      </form>
      {holidays.isError && <p data-testid="holidays-load-error">Could not load holidays.</p>}
      <ul>
        {(holidays.data ?? []).map((h) => (
          <li key={h.date} data-testid={`holiday-row-${h.date}`}>
            {h.date}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 9, Component: Holidays });
