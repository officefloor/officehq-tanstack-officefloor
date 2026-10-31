import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, type Person } from '../people/usePeople';
import { useRowAction } from '../people/useRowAction';

/**
 * A person's retirement: whether they are retired and from when, and the date and button to retire
 * them (a blank date is the sheet's today). Set at all, they count as retired at once, whatever the
 * date: no claim dated after it is accepted, and the list hides them unless retired people are shown.
 */
function PersonRetire({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const [from, setFrom] = useState('');
  const retire = useMutation({
    mutationFn: (date: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/retire`, { from: date, user }),
    onSuccess: () => setFrom(''),
    onSettled: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });
  const actionTaken = useRowAction(sheet, person.id, retire.reset);
  return (
    <>
      <td data-testid="person-retired">{person.retired ? 'yes' : 'no'}</td>
      <td data-testid="person-retired-from">{person.retiredFrom ?? ''}</td>
      <td>
        <input
          type="text"
          placeholder="yyyy-mm-dd"
          data-testid="person-retire-date"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            retire.reset();
          }}
        />{' '}
        <button
          type="button"
          data-testid="person-retire"
          disabled={retire.isPending}
          onClick={() => {
            actionTaken();
            retire.mutate(from);
          }}
        >
          Retire
        </button>
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
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 30, Component: PersonRetire }),
];
