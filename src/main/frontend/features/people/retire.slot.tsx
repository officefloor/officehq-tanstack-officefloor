import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { peoplePath, peopleQueryKey, type Person } from './people';
import { poolsQueryKey } from '../pools/pools';

// Whether a person is retired and from when, and the date and button that retire them (a blank date is the
// sheet's today). A person with a date set counts as retired at once, whatever the date.
function RetiredHeader() {
  return <th>Retired</th>;
}

function PersonRetired({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const today = useSheetToday();
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();
  const retire = useMutation({
    mutationFn: (body: { date: string }) => putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/retire`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
      await queryClient.invalidateQueries({ queryKey: poolsQueryKey(sheet ?? '') });
      setDate('');
    },
  });
  const submit = useSubmitOnce(retire);
  const rowAction = useRowAction(`person-${person.id}`);
  return (
    <td>
      <span data-testid="person-retired">{person.retiredFrom !== null ? 'yes' : 'no'}</span>{' '}
      <span data-testid="person-retired-from">{person.retiredFrom ?? ''}</span>{' '}
      <input
        data-testid="person-retire-date"
        placeholder={today ?? 'YYYY-MM-DD'}
        value={date}
        onChange={(e) => { setDate(e.target.value); retire.reset(); }}
      />
      <button type="button" data-testid="person-retire" disabled={retire.isPending} onClick={() => { rowAction.take(); submit({ date }); }}>
        Retire
      </button>
      {retire.isError && rowAction.isLatest && <span data-testid="person-error">The retirement date must be a calendar date.</span>}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 40, Component: RetiredHeader }),
  PersonCells.fill({ order: 40, Component: PersonRetired }),
];
