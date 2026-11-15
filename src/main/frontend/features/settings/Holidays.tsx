import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { addHoliday, holidaysKey, listHolidays } from './api';

/** The sheet's holidays (by date) and a form to add one. A claim's due-by, fixed when it is created,
 *  that falls on a holiday moves to the next day that is not one. */
export function Holidays({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const holidays = useQuery({ queryKey: holidaysKey(sheetId), queryFn: () => listHolidays(sheetId) });
  const [date, setDate] = useState('');

  const save = useMutation({
    mutationFn: (input: string) => addHoliday(sheetId, user, input),
    onSuccess: () => {
      setDate('');
      return queryClient.invalidateQueries({ queryKey: holidaysKey(sheetId) });
    },
  });

  const submitOnce = useSubmitOnce();

  return (
    <section>
      <h3>Holidays</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = date;
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(input);
          });
        }}
      >
        <input data-testid="holiday-date" placeholder="Date (yyyy-mm-dd)" value={date}
          onChange={(e) => {
            setDate(e.target.value);
            save.reset();
          }} />{' '}
        <button type="submit" data-testid="holiday-add" disabled={save.isPending}>
          Add holiday
        </button>
        {save.isError && (
          <span data-testid="holiday-error">A holiday needs a date not already on the list.</span>
        )}
      </form>
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
