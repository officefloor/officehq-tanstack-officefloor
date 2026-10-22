import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';

/** A date on the sheet's list of holidays. */
export type Holiday = { id: number; date: string; by: string };

export const holidaysKey = (sheetId: string) => ['sheet', sheetId, 'holidays'] as const;

const DATE = /^\d{4}-\d{2}-\d{2}$/;

function HolidayList({ sheetId }: { sheetId: string }) {
  const holidays = useQuery({
    queryKey: holidaysKey(sheetId),
    queryFn: () => getJson<Holiday[]>(`/api/sheets/${encodeURIComponent(sheetId)}/holidays`),
  }).data ?? [];
  return (
    <ul>
      {holidays.map((h) => (
        <li key={h.id} data-testid={`holiday-row-${h.date}`}>
          {h.date}
        </li>
      ))}
    </ul>
  );
}

function HolidayForm({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [date, setDate] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (d: string) => postJson<Holiday>(`/api/sheets/${encodeURIComponent(sheetId)}/holidays`, { user, date: d }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: holidaysKey(sheetId) });
      setDate('');
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not add the holiday.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!DATE.test(date.trim())) {
      setError('The holiday must be a date (yyyy-mm-dd).');
      return;
    }
    if (!once.start()) return;
    save.mutate(date.trim());
  };

  return (
    <form data-testid="holiday-form" onSubmit={submit}>
      <input
        data-testid="holiday-date"
        placeholder="yyyy-mm-dd"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          setError('');
        }}
      />{' '}
      <button type="submit" data-testid="holiday-add" disabled={save.isPending}>
        Add holiday
      </button>
      {error && (
        <p data-testid="holiday-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

/** The sheet's holidays and the entry a date is added with. A new claim's due-by falling on a holiday is moved to
 *  the next day that is not one; claims already made keep theirs. */
function Holidays({ sheetId, user }: { sheetId: string; user: string }) {
  return (
    <section data-testid="holidays">
      <h2>Holidays</h2>
      <HolidayForm sheetId={sheetId} user={user} />
      <p>A new claim due by a holiday is due by the next day that is not one.</p>
      <HolidayList sheetId={sheetId} />
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7.5, Component: Holidays });
