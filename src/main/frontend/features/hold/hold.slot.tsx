import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/usePeople';

/** A person's holds: whether they have any, a line per hold (from – to), and a from-date (blank: the
 *  sheet's today), an optional to-date (blank: open-ended) and a button that adds a hold. Their open
 *  (OPEN or PENDING) claims dated within a hold, unless bound by a statement, count in no live total;
 *  their other claims still do. A hold ends on its to-date. */
function PersonHold({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const p = people.data?.find((x) => x.id === personId);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const hold = useMutation({
    mutationFn: (body: { from: string; to: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/hold`, body),
    onSuccess: async () => {
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });

  return (
    <>
      <td data-testid="person-hold">{p?.held ? 'yes' : 'no'}</td>
      <td>
        {(p?.holds ?? []).map((h, i) => (
          <div key={i} data-testid="person-hold-line">
            {h.from ?? ''} – {h.to ?? ''}
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
            hold.reset();
            hold.mutate({ from: from.trim(), to: to.trim() });
          }}
        >
          Hold
        </button>
        {hold.isError && <span data-testid="person-error">{hold.error.message}</span>}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHead.fill({
    order: 45,
    Component: () => (
      <>
        <th>Held</th>
        <th>Holds</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 45, Component: PersonHold }),
];
