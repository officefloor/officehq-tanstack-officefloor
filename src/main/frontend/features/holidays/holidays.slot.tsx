import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { holidaysKey, useHolidays, type Holiday } from './useHolidays';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * The sheet's holidays and a date entry to add one. A claim's due-by fixed at creation that falls
 * on a holiday moves to the next day that is not one; a date already on the list is refused.
 */
function HolidaysPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: holidays = [] } = useHolidays(sheet);
  const [date, setDate] = useState('');
  const [invalid, setInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (d: string) => postJson<Holiday>(`/api/sheets/${encodeURIComponent(sheet)}/holidays`, { date: d, user }),
    onSuccess: async () => {
      setDate('');
      await queryClient.invalidateQueries({ queryKey: holidaysKey(sheet) });
      // A new holiday re-fixes the due-by of the claims due by it or later.
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });

  // An error goes as soon as its form is changed or submitted again.
  const clearError = () => {
    add.reset();
    setInvalid(false);
  };

  return (
    <section data-testid="holidays-panel">
      <h2>Holidays</h2>
      <form
        data-testid="holiday-form"
        onSubmit={(e) => {
          e.preventDefault();
          const d = date.trim();
          if (!DATE.test(d) || holidays.some((h) => h.date === d)) {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            add.reset();
            setInvalid(true);
            return;
          }
          setInvalid(false);
          submitOnce(() => add.mutateAsync(d));
        }}
      >
        <label>
          Date{' '}
          <input
            data-testid="holiday-date"
            placeholder="yyyy-mm-dd"
            value={date}
            onChange={(e) => {
              setDate(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="holiday-add" disabled={add.isPending}>
          Add holiday
        </button>
        {(invalid || add.isError) && (
          <span data-testid="holiday-error" role="alert">
            {' '}
            {invalid ? 'Enter a date (yyyy-mm-dd) not already on the list.' : 'Could not add this holiday.'}
          </span>
        )}
      </form>
      <ul data-testid="holiday-list">
        {holidays.map((h) => (
          <li key={h.date} data-testid={`holiday-row-${h.date}`}>
            {h.date}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 7, Component: HolidaysPanel });
