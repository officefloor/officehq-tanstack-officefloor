import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { DATE } from '../settings/settings';
import { holidaysKey, useHolidays } from './holidays';
import { useOneAtATime } from '../../query/useOneAtATime';

// The sheet's holidays and the entry a date is added to them with; a date already on the list is refused.
export function Holidays() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const holidays = useHolidays().data ?? [];
  const [date, setDate] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (input: { date: string }) =>
      postJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/holidays?user=${encodeURIComponent(user)}`, input),
    onSuccess: async (saved) => {
      setDate('');
      setError('');
      queryClient.setQueryData(holidaysKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: holidaysKey(sheet) });
    },
    onError: () => setError('That holiday was refused: the date is already on the list.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const day = date.trim();
    if (!DATE.test(day)) {
      setError('Type the date as yyyy-mm-dd.');
      return;
    }
    if (holidays.includes(day)) {
      setError(`${day} is already a holiday.`);
      return;
    }
    once(() => add.mutateAsync({ date: day }));
  };

  return (
    <section data-testid="holidays">
      <ul data-testid="holiday-list">
        {holidays.map((h) => (
          <li key={h} data-testid={`holiday-row-${h}`}>
            {h}
          </li>
        ))}
      </ul>
      <form onSubmit={submit} data-testid="holiday-form">
        <input
          data-testid="holiday-date"
          placeholder="Holiday (yyyy-mm-dd)"
          value={date}
          onChange={(e) => {
            setDate(e.target.value);
            setError('');
          }}
        />
        <button type="submit" data-testid="holiday-add" disabled={add.isPending}>
          Add holiday
        </button>
        {error && <span data-testid="holiday-error">{error}</span>}
      </form>
    </section>
  );
}
