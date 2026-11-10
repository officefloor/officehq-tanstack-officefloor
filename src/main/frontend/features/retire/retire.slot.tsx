import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { useRowError } from '../people/rowError';

function RetireHead() {
  return (
    <>
      <th>Retired</th>
      <th>Retired from</th>
      <th />
    </>
  );
}

// A person's retirement: whether they are retired and from when, and a date (blank: the sheet's
// today) with a button that retires them from it. No claim dated after it can be added to them.
function PersonRetire({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [from, setFrom] = useState('');
  const [error, setError] = useRowError(person.id, 'retire');
  const once = useOnce();
  const retire = useMutation({
    mutationFn: (date: string) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/retire`,
        { from: date },
        user,
      ),
    onSuccess: () => {
      setFrom('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: () => setError('That person could not be retired.'),
  });

  const submit = () => {
    setError('');
    once((done) => retire.mutate(from.trim(), { onSettled: done }));
  };

  return (
    <>
      <td data-testid="person-retired">{person.retired ? 'yes' : 'no'}</td>
      <td data-testid="person-retired-from">{person.retiredFrom}</td>
      <td>
        <input
          type="date"
          data-testid="person-retire-date"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="button" data-testid="person-retire" onClick={submit} disabled={retire.isPending}>
          Retire
        </button>
        {error && (
          <p role="alert" data-testid="person-error">
            {error}
          </p>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 40, Component: RetireHead }),
  PersonRow.fill({ order: 40, Component: PersonRetire }),
];
