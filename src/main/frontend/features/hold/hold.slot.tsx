import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { useUser } from '../session/session';

function HoldHead() {
  return (
    <>
      <th>On hold</th>
      <th />
    </>
  );
}

// Whether a person is on hold, and a button that puts them on it or takes them off it. While held,
// their OPEN claims count in no total; their SETTLED claims still do.
function PersonHold({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useState('');
  const once = useOnce();
  const hold = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/hold`,
        {},
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
    onError: () => setError('That person could not be put on or taken off hold.'),
  });

  const toggle = () => {
    setError('');
    once((done) => hold.mutate(undefined, { onSettled: done }));
  };

  return (
    <>
      <td data-testid="person-hold">{person.held ? 'yes' : 'no'}</td>
      <td>
        <button type="button" data-testid="person-hold-toggle" onClick={toggle} disabled={hold.isPending}>
          {person.held ? 'Release' : 'Hold'}
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
