import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { claimsQueryKey } from '../claims/claims';
import { peopleQueryKey } from '../people/people';
import { holidaysPath, holidaysQueryKey, useHolidays, type Holiday } from './holidays';

// The sheet's holidays, and the entry that adds one. A date that is not a calendar date, or one already on the
// list, is refused by the server and shows the error beside the entry, which goes as soon as the entry changes.
function Holidays() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const holidays = useHolidays();
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { date: string }) => postJson<Holiday>(holidaysPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: holidaysQueryKey(sheet ?? '') });
      // Adding a holiday can move claims' due-by, and so their figures.
      await queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet ?? '') });
      await queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
      setDate('');
    },
  });
  const submit = useSubmitOnce(add);

  if (!user || !sheet) return null;
  return (
    <section data-testid="holidays">
      <h2>Holidays</h2>
      <ul>
        {(holidays.data ?? []).map((h) => (
          <li key={h.id} data-testid={`holiday-row-${h.date}`}>{h.date}</li>
        ))}
      </ul>
      <form
        data-testid="holiday-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ date });
        }}
      >
        <input
          data-testid="holiday-date"
          placeholder="Date (YYYY-MM-DD)"
          value={date}
          onChange={(e) => { setDate(e.target.value); add.reset(); }}
        />
        <button type="submit" data-testid="holiday-add" disabled={add.isPending}>
          Add holiday
        </button>
        {add.isError && (
          <span data-testid="holiday-error">A holiday must be a calendar date not already on the list.</span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 6.5, Component: Holidays });
