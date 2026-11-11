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

function HoldHead() {
  return (
    <>
      <Col name="hold"><th>On hold</th></Col>
      <th>Holds</th>
      <th />
    </>
  );
}

// Whether a person has any hold, their holds, and a from-date (blank: the sheet's today) and an
// optional to-date with a button that adds a hold over them. An OPEN or PENDING claim of theirs
// dated within a hold counts in no total, unless a statement binds it; a hold ends on its to-date.
function PersonHold({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useRowError(person.id, 'hold');
  const once = useOnce();
  const hold = useMutation({
    mutationFn: (dates: { from: string; to: string }) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/hold`,
        dates,
        user,
      ),
    onSuccess: () => {
      setFrom('');
      setTo('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: () => setError('That person could not be put on hold.'),
  });

  const submit = () => {
    setError('');
    once((done) => hold.mutate({ from: from.trim(), to: to.trim() }, { onSettled: done }));
  };

  return (
    <>
      <Col name="hold"><td data-testid="person-hold">{person.held ? 'yes' : 'no'}</td></Col>
      <td>
        {person.holds.map((h, i) => (
          <div key={i} data-testid="person-hold-line">
            {h.from} to {h.to === '' ? 'open' : h.to}
          </div>
        ))}
      </td>
      <td>
        <input
          type="date"
          data-testid="person-hold-from"
          aria-label="Hold from"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            setError('');
          }}
        />{' '}
        <input
          type="date"
          data-testid="person-hold-to"
          aria-label="Hold to"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="button" data-testid="person-hold-toggle" onClick={submit} disabled={hold.isPending}>
          Hold
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
  PersonHead.fill({ order: 50, Component: HoldHead }),
  PersonRow.fill({ order: 50, Component: PersonHold }),
];
