import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { holidaysKey, holidaysQuery } from './holidaysQuery';
import type { Holiday } from './holidaysQuery';

/** The sheet's holidays, and the entry a date is added through. A claim's due-by, fixed when it is made, that falls on
 *  a holiday then on the list is the next day that is not one; a holiday added later leaves due-bys as they are. A date
 *  already on the list is refused. */
function Holidays({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const holidays = useQuery(holidaysQuery(sheetId));
  const [date, setDate] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: Holiday) =>
      postJson<Holiday>(`/api/sheets/${encodeURIComponent(sheetId)}/holidays`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: holidaysKey(sheetId) });
      setDate('');
    },
    onError: (e: Error) => setError(e.message || 'The holiday could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => save.mutateAsync({ date: date.trim() }));
  };

  return (
    <section data-testid="holidays">
      <h3>Holidays</h3>
      <form onSubmit={submit}>
        <label>
          Date{' '}
          <input
            data-testid="holiday-date"
            placeholder="yyyy-mm-dd"
            value={date}
            onChange={(e) => {
              setDate(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="holiday-add" disabled={save.isPending}>
          Add holiday
        </button>
        {error && <p data-testid="holiday-error">{error}</p>}
      </form>
      {holidays.isError && <p data-testid="holidays-load-error">Could not load the holidays.</p>}
      <ul data-testid="holidays-list">
        {(holidays.data ?? []).map((h) => (
          <li key={h.date} data-testid={`holiday-row-${h.date}`}>
            {h.date}
          </li>
        ))}
      </ul>
    </section>
  );
}

// Keyed by sheet, so a typed value or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 6.5,
  Component: ({ sheetId }) => <Holidays key={`holidays-${sheetId}`} sheetId={sheetId} />,
});
