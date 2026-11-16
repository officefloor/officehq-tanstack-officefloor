import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { peopleKey, type Person } from '../people/people';
import { usePersonRowAction } from '../people/rowAction';

function RetireHead() {
  return (
    <>
      <th>Retired</th>
      <th>Retired from</th>
      <th />
    </>
  );
}

/** Whether the person is retired and from when, and the date and button that retire them (a blank date: the sheet's
 *  today). A person with a retirement date counts as retired at once, whatever the date; no claim of theirs may be
 *  dated after it. A refused retirement shows the row's error. */
function PersonRetire({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [from, setFrom] = useState('');
  const submit = useSubmitOnce();
  const rowAction = usePersonRowAction(sheetId, person.id, 'retire');
  const retire = useMutation({
    mutationFn: (date: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/retire`, { from: date, by: user }),
    onSuccess: () => {
      setFrom('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <>
      <td data-testid="person-retired">{person.retiredFrom !== null ? 'yes' : 'no'}</td>
      <td data-testid="person-retired-from">{person.retiredFrom ?? ''}</td>
      <td>
        <input
          data-testid="person-retire-date"
          placeholder="yyyy-MM-dd"
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
            if (!submit.begin()) return;
            rowAction.take();
            retire.reset();
            retire.mutate(from.trim());
          }}
        >
          Retire
        </button>
        {retire.isError && rowAction.isLatest && <span data-testid="person-error"> Could not retire this person.</span>}
      </td>
    </>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 40, Component: RetireHead }),
  PersonRow.fill({ order: 40, Component: PersonRetire }),
];
