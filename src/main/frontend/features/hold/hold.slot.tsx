import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';
import { useUser } from '../session/useSession';

function HoldHeads() {
  return (
    <>
      <th>On hold</th>
      <th>Holds</th>
      <th />
    </>
  );
}

// Whether the person has any hold, their holds (each from its from-date to its to-date, none: open-ended), and the
// dates and button that add one (a blank from-date: the sheet's today). Their OPEN and PENDING claims dated within a
// hold count in no live total unless a statement binds them; a hold ends on its to-date, there is no release. A
// refusal shows on the person until the next action on their row.
function HoldCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const person = usePeople(sheetId).data?.find((p) => p.id === personId);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const hold = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: (dates: { from: string; to: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/hold`, {
        from: dates.from.trim(),
        to: dates.to.trim(),
        by: user ?? '',
      }),
    onSuccess: async () => {
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });
  const last = useLastPersonAction(sheetId, personId, hold.submittedAt);

  return (
    <>
      <td data-testid="person-hold">{person?.held ? 'yes' : 'no'}</td>
      <td>
        {(person?.holds ?? []).map((h, i) => (
          <div key={i} data-testid="person-hold-line">
            {h.from ?? '…'} – {h.to ?? '…'}
          </div>
        ))}
      </td>
      <td>
        <input
          type="date"
          data-testid="person-hold-from"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            hold.reset();
          }}
        />{' '}
        <input
          type="date"
          data-testid="person-hold-to"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            hold.reset();
          }}
        />{' '}
        <button type="button" data-testid="person-hold-toggle" disabled={hold.isPending} onClick={() => hold.mutate({ from, to })}>
          Hold
        </button>
        {hold.isError && last && (
          <span data-testid="person-error" role="alert">
            {hold.error.message}
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 45, Component: HoldHeads }),
  PersonCells.fill({ order: 45, Component: HoldCells }),
];
