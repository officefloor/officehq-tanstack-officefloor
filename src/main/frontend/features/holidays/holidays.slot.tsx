import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { DATE, holidaysKey, useHolidays } from './holidays';
import type { Holiday } from './holidays';

// The sheet's holidays: a date entry and an add button, each holiday listed below. A date already on the
// list is refused. A claim's due-by, fixed when it is created, counts its terms in business days, skipping
// Saturdays, Sundays and the holidays on the list then; a holiday added later leaves existing due-by dates as they are.
function Holidays({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const holidays = useHolidays(sheetId);
  const [date, setDate] = useState('');
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const add = useMutation({
    mutationFn: (body: { date: string }) =>
      postJson<Holiday>(`/api/sheets/${encodeURIComponent(sheetId)}/holidays`, body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: holidaysKey(sheetId) }),
    onError: () => setError('That holiday could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const value = date.trim();
    if (!DATE.test(value) || Number.isNaN(Date.parse(value))) {
      setError('A date is yyyy-MM-dd.');
      return;
    }
    if ((holidays.data ?? []).some((h) => h.date === value)) {
      setError(`${value} is already a holiday.`);
      return;
    }
    once((done) => add.mutate({ date: value }, { onSettled: done }));
  };

  return (
    <section data-testid="holidays">
      <h2>Holidays</h2>
      <form onSubmit={submit}>
        <label>
          Date{' '}
          <input
            data-testid="holiday-date"
            placeholder="yyyy-MM-dd"
            value={date}
            onChange={(e) => {
              setDate(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="holiday-add" disabled={add.isPending}>
          Add holiday
        </button>
        {error && (
          <p role="alert" data-testid="holiday-error">
            {error}
          </p>
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

export const contribution = SheetBody.fill({ order: -6, Component: Holidays });
