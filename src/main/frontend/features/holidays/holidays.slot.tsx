import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Holiday, holidaysKey, useHolidays } from './holidays';

// The sheet's holidays, and the entry a date is added with. A date already on the list is refused.
function Holidays() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const holidays = useHolidays(sheet);
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationFn: (d: string) =>
      postJson<Holiday>(`/api/sheets/${encodeURIComponent(sheet)}/holidays`, { date: d, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: holidaysKey(sheet) });
      setDate('');
    },
  });
  const submit = useSubmitOnce(add);

  return (
    <section data-testid="holidays">
      <h2>Holidays</h2>
      <ul>
        {(holidays.data ?? []).map((h) => (
          <li key={h.date} data-testid={`holiday-row-${h.date}`}>
            {h.date}
          </li>
        ))}
      </ul>
      <form
        data-testid="holiday-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit(date);
        }}
      >
        <input
          data-testid="holiday-date"
          placeholder="Date (yyyy-mm-dd)"
          value={date}
          onChange={(e) => {
            setDate(e.target.value);
            add.reset();
          }}
        />
        <button type="submit" data-testid="holiday-add" disabled={add.isPending}>
          Add holiday
        </button>
        {add.isError && (
          <span data-testid="holiday-error" role="alert">
            {' '}
            {add.error instanceof HttpError && add.error.reason ? `${add.error.reason}.` : 'The holiday was refused.'}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 47, Component: Holidays });
