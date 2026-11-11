import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { useRowError } from '../people/rowError';
import { Col } from '../columns/columns';

function RetireHead() {
  return (
    <>
      <Col name="retired"><th>Retired</th></Col>
      <th>Retired from</th>
      <th>Past retirements</th>
      <th />
    </>
  );
}

// A person's retirement: whether they are retired and from when, and a date (blank: the sheet's
// today) with a button that retires them from it. No claim dated after it can be added to them.
// A retired person can be unretired (on the sheet's today); each retirement they were unretired
// from is kept on the row, one line each, and no claim dated within it can be added to them.
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
  const unretire = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/unretire`,
        {},
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
    onError: () => setError('That person could not be unretired.'),
  });

  const submit = () => {
    setError('');
    once((done) => retire.mutate(from.trim(), { onSettled: done }));
  };

  const submitUnretire = () => {
    setError('');
    once((done) => unretire.mutate(undefined, { onSettled: done }));
  };

  return (
    <>
      <Col name="retired"><td data-testid="person-retired">{person.retired ? 'yes' : 'no'}</td></Col>
      <td data-testid="person-retired-from">{person.retiredFrom}</td>
      <td>
        {person.retirements.map((r, i) => (
          <div key={i} data-testid="person-retirement-line">
            {r}
          </div>
        ))}
      </td>
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
        </button>{' '}
        <button type="button" data-testid="person-unretire" onClick={submitUnretire} disabled={unretire.isPending}>
          Unretire
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
