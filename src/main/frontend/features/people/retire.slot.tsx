import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useIsLatest } from '../../query/useIsLatest';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { isRetired, peopleKey, peoplePath, personActionKey } from './people';
import type { Person } from './people';

function RetireHeader() {
  return (
    <>
      <th>Retired</th>
      <th>Retired from</th>
      <th />
    </>
  );
}

// Whether the person is retired and from when, and the date and button that retire them (a blank date is the
// sheet's today). Once retired, a claim dated after that date is refused.
function PersonRetire({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [from, setFrom] = useState('');
  const retire = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (date: string) =>
      postJson<Person[]>(byUser(`${peoplePath(sheet)}/${person.id}/retire`, user), { from: date }),
    onSuccess: (updated) => {
      queryClient.setQueryData(peopleKey(sheet), updated);
      setFrom('');
    },
  });
  const act = useSubmitOnce(retire);
  const latest = useIsLatest(personActionKey(sheet, person.id), retire.submittedAt);

  return (
    <>
      <td data-testid="person-retired">{isRetired(person) ? 'yes' : 'no'}</td>
      <td data-testid="person-retired-from">{person.retiredFrom ?? ''}</td>
      <td>
        <input
          data-testid="person-retire-date"
          type="text"
          placeholder="yyyy-mm-dd"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            retire.reset();
          }}
        />{' '}
        <button type="button" data-testid="person-retire" disabled={retire.isPending} onClick={() => act(from.trim())}>
          Retire
        </button>
        {retire.isError && latest && (
          <span data-testid="person-error" role="alert">
            {' '}That retirement date was refused.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 30, Component: RetireHeader }),
  PersonColumns.fill({ order: 30, Component: PersonRetire }),
];
