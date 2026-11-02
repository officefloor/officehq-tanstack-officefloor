import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useIsLatest } from '../../query/useIsLatest';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { useShown } from '../columns/columns';
import { isRetired, peopleKey, peoplePath, personActionKey } from './people';
import type { Person } from './people';

function RetireHeader({ sheet, user }: { sheet: string; user: string }) {
  const shown = useShown(sheet, user);
  return (
    <>
      {shown('retired') && <th>Retired</th>}
      <th>Retired from</th>
      <th />
      <th>Past retirements</th>
    </>
  );
}

// Whether the person is retired and from when, and the date and button that retire them (a blank date is the
// sheet's today), and the button that unretires them on the sheet's today (anyone not retired is refused). Once
// retired, a claim dated after that date is refused; each past retirement is kept, one line each, and a claim dated
// after its from and before its to is refused too.
function PersonRetire({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const shown = useShown(sheet, user);
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
  const unretire = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: () => postJson<Person[]>(byUser(`${peoplePath(sheet)}/${person.id}/unretire`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(peopleKey(sheet), updated),
  });
  const actUnretire = useSubmitOnce(unretire);
  const latestUnretire = useIsLatest(personActionKey(sheet, person.id), unretire.submittedAt);

  return (
    <>
      {shown('retired') && <td data-testid="person-retired">{isRetired(person) ? 'yes' : 'no'}</td>}
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
        )}{' '}
        <button
          type="button"
          data-testid="person-unretire"
          disabled={unretire.isPending}
          onClick={() => actUnretire(undefined)}
        >
          Unretire
        </button>
        {unretire.isError && latestUnretire && (
          <span data-testid="person-error" role="alert">
            {' '}Only a retired person can be unretired.
          </span>
        )}
      </td>
      <td>
        {person.retirements.map((r, i) => (
          <div key={i} data-testid="person-retirement-line">
            {r.from} to {r.to}
          </div>
        ))}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 30, Component: RetireHeader }),
  PersonColumns.fill({ order: 30, Component: PersonRetire }),
];
