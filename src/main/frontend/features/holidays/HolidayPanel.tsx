import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { holidaysKey, useHolidays } from './useHolidays';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/usePeople';

// The sheet's holidays and the form that adds one (a date already on the list is refused). A claim's
// due-by fixed at creation that falls on a holiday moves to the next day that is not one.
export function HolidayPanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const holidays = useHolidays(sheet);
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { date: string }) =>
      postJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/holidays`, { ...body, user }),
    onSuccess: (saved) => {
      setDate('');
      queryClient.setQueryData(holidaysKey(sheet), saved);
      // Adding a holiday re-fixes the due-by of the claims after it, which can move their figures.
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });
  return (
    <>
      <ul data-testid="holiday-list">
        {holidays.map((h) => (
          <li key={h} data-testid={`holiday-row-${h}`}>{h}</li>
        ))}
      </ul>
      <form
        data-testid="holiday-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            add.reset();
            return add.mutateAsync({ date: date.trim() });
          });
        }}
      >
        <input
          data-testid="holiday-date"
          value={date}
          placeholder="Holiday (YYYY-MM-DD)"
          onChange={(e) => {
            setDate(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="holiday-add">Add holiday</button>
        {add.isError && <span data-testid="holiday-error" role="alert"> Could not add the holiday: it must be a date (YYYY-MM-DD) not already on the list.</span>}
      </form>
    </>
  );
}
