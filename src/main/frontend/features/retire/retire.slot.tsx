import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { peopleKey, usePeople, type Person } from '../people/usePeople';
import { useShowRetired } from './useShowRetired';

/** A person's retirement: whether they are retired and from when, and a date (blank: the sheet's
 *  today) and button that retire them. A person with a date set counts as retired at once; no claim
 *  of theirs may be dated after it, and the list leaves them out unless "show retired" is on. */
function PersonRetire({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const p = people.data?.find((x) => x.id === personId);
  const [date, setDate] = useState('');

  const retire = useMutation({
    mutationFn: (raw: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/retire`, { date: raw.trim() }),
    onSuccess: async () => {
      setDate('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });

  return (
    <>
      <td data-testid="person-retired">{p?.retiredFrom ? 'yes' : 'no'}</td>
      <td data-testid="person-retired-from">{p?.retiredFrom ?? ''}</td>
      <td>
        <input
          type="text"
          placeholder="yyyy-mm-dd"
          data-testid="person-retire-date"
          value={date}
          onChange={(e) => {
            setDate(e.target.value);
            retire.reset();
          }}
        />{' '}
        <button
          type="button"
          data-testid="person-retire"
          disabled={retire.isPending}
          onClick={() => {
            retire.reset();
            retire.mutate(date);
          }}
        >
          Retire
        </button>
        {retire.isError && <span data-testid="person-error">{retire.error.message}</span>}
      </td>
    </>
  );
}

/** The "show retired" toggle: off when the screen is first opened. */
function ShowRetired() {
  const [on, setOn] = useShowRetired();
  return (
    <label>
      <input type="checkbox" data-testid="show-retired" checked={on} onChange={(e) => setOn(e.target.checked)} /> Show
      retired
    </label>
  );
}

export const contributions = [
  SheetBody.fill({ order: 5, Component: () => <ShowRetired /> }),
  PersonColumnHead.fill({
    order: 40,
    Component: () => (
      <>
        <th>Retired</th>
        <th>Retired from</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 40, Component: PersonRetire }),
];
