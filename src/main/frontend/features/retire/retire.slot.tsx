import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';
import { useUser } from '../session/useSession';

function RetireHeads() {
  return (
    <>
      <th>Retired</th>
      <th>Retired from</th>
      <th />
    </>
  );
}

// Whether the person is retired and from when, and the date and button that retire them (a blank date: the
// sheet's today). A person with a retirement date counts as retired at once, whatever the date. A refusal shows
// on the person until the next action on their row.
function RetireCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const person = usePeople(sheetId).data?.find((p) => p.id === personId);
  const [date, setDate] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const retire = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: (from: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/retire`, {
        date: from.trim(),
        by: user ?? '',
      }),
    onSuccess: async () => {
      setDate('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });
  const last = useLastPersonAction(sheetId, personId, retire.submittedAt);
  const retired = person?.retiredFrom != null;

  return (
    <>
      <td data-testid="person-retired">{retired ? 'yes' : 'no'}</td>
      <td data-testid="person-retired-from">{person?.retiredFrom ?? ''}</td>
      <td>
        <input
          type="date"
          data-testid="person-retire-date"
          value={date}
          onChange={(e) => {
            setDate(e.target.value);
            retire.reset();
          }}
        />{' '}
        <button type="button" data-testid="person-retire" disabled={retire.isPending} onClick={() => retire.mutate(date)}>
          Retire
        </button>
        {retire.isError && last && (
          <span data-testid="person-error" role="alert">
            {retire.error.message}
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 40, Component: RetireHeads }),
  PersonCells.fill({ order: 40, Component: RetireCells }),
];
