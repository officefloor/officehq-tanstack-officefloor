import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { usePersonError } from './usePersonError';
import { useUser } from '../session/useUser';
import { peopleKey, peopleQuery } from './peopleQuery';
import type { Person } from './peopleQuery';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Whether the person is retired, the date they are retired from, and the control that retires them
 * from a date (a blank date is the sheet's today). A person with a date set is retired at once. A retired person can
 * be unretired (on the sheet's today); their past retirements are listed, one line each.
 */
function PersonRetire({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [date, setDate] = useState('');
  const [, setError] = usePersonError(personId);
  const once = useSubmitOnce();
  const person = (useQuery(peopleQuery(sheetId)).data ?? []).find((p) => p.id === personId);
  const retire = useMutation({
    mutationFn: (from: string) =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/retire`, { from }, user),
    onSuccess: async () => {
      setDate('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: () => setError('The person could not be retired.'),
  });
  const unretire = useMutation({
    mutationFn: () =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/unretire`, {}, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: () => setError('Only a retired person can be unretired.'),
  });

  return (
    <td>
      <span data-testid="person-retired">{person?.retiredFrom ? 'yes' : 'no'}</span>{' '}
      <span data-testid="person-retired-from">{person?.retiredFrom ?? ''}</span>{' '}
      <input
        data-testid="person-retire-date"
        placeholder="yyyy-mm-dd"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          setError('');
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-retire"
        disabled={retire.isPending}
        onClick={() => {
          setError('');
          const d = date.trim();
          if (d !== '' && !DATE.test(d)) {
            setError('The date must be yyyy-mm-dd.');
            return;
          }
          once(() => retire.mutateAsync(d));
        }}
      >
        Retire
      </button>{' '}
      <button
        type="button"
        data-testid="person-unretire"
        disabled={unretire.isPending}
        onClick={() => {
          setError('');
          once(() => unretire.mutateAsync());
        }}
      >
        Unretire
      </button>
      {(person?.retirements ?? []).map((r, i) => (
        <div key={i} data-testid="person-retirement-line">
          {r.from} to {r.to}
        </div>
      ))}
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 18, Component: () => <th>Retired</th> }),
  PersonCells.fill({ order: 18, Component: PersonRetire }),
];
