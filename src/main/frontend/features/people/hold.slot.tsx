import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { isHeld, peopleKey, peoplePath } from './people';
import type { Person } from './people';

function HoldHeader() {
  return (
    <>
      <th>Hold</th>
      <th>Holds</th>
      <th />
    </>
  );
}

// Whether the person has any hold, each of their holds, and the dates and button that add one (a blank from-date
// is the sheet's today; a blank to-date never ends). Their OPEN claims dated within a hold count in no live total,
// unless bound by a statement; their other claims still do. A hold ends on its to-date.
function PersonHold({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const hold = useMutation({
    mutationFn: (dates: { from: string; to: string }) =>
      postJson<Person[]>(byUser(`${peoplePath(sheet)}/${person.id}/hold`, user), dates),
    onSuccess: (updated) => {
      queryClient.setQueryData(peopleKey(sheet), updated);
      setFrom('');
      setTo('');
    },
  });
  const act = useSubmitOnce(hold);

  return (
    <>
      <td data-testid="person-hold">{isHeld(person) ? 'yes' : 'no'}</td>
      <td>
        {person.holds.map((h, i) => (
          <div key={i} data-testid="person-hold-line">
            {h.from} – {h.to ?? ''}
          </div>
        ))}
      </td>
      <td>
        <input
          data-testid="person-hold-from"
          type="text"
          placeholder="from yyyy-mm-dd"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            hold.reset();
          }}
        />{' '}
        <input
          data-testid="person-hold-to"
          type="text"
          placeholder="to yyyy-mm-dd"
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
          onClick={() => act({ from: from.trim(), to: to.trim() })}
        >
          Hold
        </button>
        {hold.isError && (
          <span data-testid="person-error" role="alert">
            {' '}That hold was refused.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 40, Component: HoldHeader }),
  PersonColumns.fill({ order: 40, Component: PersonHold }),
];
