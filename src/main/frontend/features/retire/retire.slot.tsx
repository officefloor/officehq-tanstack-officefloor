import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, type Person } from '../people/usePeople';
import { poolsKey } from '../pools/usePools';
import { useRowAction } from '../people/useRowAction';

/**
 * A person's retirement: whether they are retired and from when, and the date and button to retire
 * them (a blank date is the sheet's today). Set at all, they count as retired at once, whatever the
 * date: no claim dated after it is accepted, and the list hides them unless retired people are shown.
 * A retired person can be unretired on the sheet's today: the retirement is kept as a past one, a line
 * from its date to the day unretired, and no claim dated after the from and before the to is accepted.
 */
function PersonRetire({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const [from, setFrom] = useState('');
  const retire = useMutation({
    mutationFn: (date: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/retire`, { from: date, user }),
    onSuccess: () => setFrom(''),
    // A retirement changes the pools' cuts (a retired member's share is spread over the others).
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheet) }),
      ]),
  });
  const unretire = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/unretire`, { user }),
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheet) }),
      ]),
  });
  const actionTaken = useRowAction(sheet, person.id, () => {
    retire.reset();
    unretire.reset();
  });
  return (
    <>
      <td data-testid="person-retired">{person.retired ? 'yes' : 'no'}</td>
      <td data-testid="person-retired-from">{person.retiredFrom ?? ''}</td>
      <td>
        {person.pastRetirements.map((r, i) => (
          <div key={i} data-testid="person-retirement-line">
            {r.from} to {r.to}
          </div>
        ))}
      </td>
      <td>
        <input
          type="text"
          placeholder="yyyy-mm-dd"
          data-testid="person-retire-date"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            retire.reset();
            unretire.reset();
          }}
        />{' '}
        <button
          type="button"
          data-testid="person-retire"
          disabled={retire.isPending}
          onClick={() => {
            actionTaken();
            unretire.reset();
            retire.mutate(from);
          }}
        >
          Retire
        </button>{' '}
        <button
          type="button"
          data-testid="person-unretire"
          disabled={unretire.isPending}
          onClick={() => {
            actionTaken();
            retire.reset();
            unretire.mutate();
          }}
        >
          Unretire
        </button>
        {unretire.isError && (
          <span data-testid="person-error" role="alert">
            {' '}
            Could not unretire this person.
          </span>
        )}
        {retire.isError && (
          <span data-testid="person-error" role="alert">
            {' '}
            Could not retire this person.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 30,
    Component: () => (
      <>
        <th>Retired</th>
        <th>Retired from</th>
        <th>Past retirements</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 30, Component: PersonRetire }),
];
