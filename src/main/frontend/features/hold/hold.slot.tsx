import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, type Person } from '../people/usePeople';
import { useRowAction } from '../people/useRowAction';

/**
 * A person's holds: whether they have any, a line per hold with its dates, and the dates and button
 * to add one (a blank from-date is the sheet's today, a blank to-date leaves it open-ended). Their
 * OPEN claims dated within a hold count in no live total, unless a statement binds them; their other
 * claims still count. A hold ends on its to-date: there is no release.
 */
function PersonHold({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const hold = useMutation({
    mutationFn: (dates: { from: string; to: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/hold`, { ...dates, user }),
    onSuccess: () => {
      setFrom('');
      setTo('');
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });
  const actionTaken = useRowAction(sheet, person.id, hold.reset);
  return (
    <>
      <td data-testid="person-hold">{person.held ? 'yes' : 'no'}</td>
      <td>
        {person.holds.map((h, i) => (
          <div key={i} data-testid="person-hold-line">
            {h.from} – {h.to ?? 'open'}
          </div>
        ))}
      </td>
      <td>
        <input
          type="text"
          placeholder="from yyyy-mm-dd"
          data-testid="person-hold-from"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            hold.reset();
          }}
        />{' '}
        <input
          type="text"
          placeholder="to yyyy-mm-dd"
          data-testid="person-hold-to"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            hold.reset();
          }}
        />{' '}
        <button
          type="button"
          data-testid="person-hold-toggle"
          disabled={hold.isPending}
          onClick={() => {
            actionTaken();
            hold.mutate({ from, to });
          }}
        >
          Hold
        </button>
        {hold.isError && (
          <span data-testid="person-error" role="alert">
            {' '}
            Could not add this hold.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 35,
    Component: () => (
      <>
        <th>Held</th>
        <th>Holds</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 35, Component: PersonHold }),
];
