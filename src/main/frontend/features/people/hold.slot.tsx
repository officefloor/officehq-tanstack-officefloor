import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useRealToday } from '../session/session';
import { peoplePath, peopleQueryKey, type Person } from './people';
import { useColumnShown } from './columns';

// Whether a person has any hold, a line for each of their holds, and the from-date (blank: the sheet's today),
// to-date (blank: open-ended) and button that add a hold. Their OPEN claims dated within a hold count in no live
// total, unless a statement binds them; their other claims still count. A hold ends on its to-date.
function HoldHeader() {
  return <th>Hold</th>;
}

function PersonHold({ person }: { person: Person }) {
  const shown = useColumnShown('hold');
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const today = useRealToday();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();
  const hold = useMutation({
    mutationFn: (body: { from: string; to: string }) =>
      putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/hold`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
      setFrom('');
      setTo('');
    },
  });
  const submit = useSubmitOnce(hold);
  const rowAction = useRowAction(`person-${person.id}`);
  return (
    <td>
      {shown && <><span data-testid="person-hold">{person.held ? 'yes' : 'no'}</span>{' '}</>}
      {person.holds.map((h, i) => (
        <span key={i} data-testid="person-hold-line">{h.from} – {h.to ?? 'open'}{' '}</span>
      ))}
      <input
        data-testid="person-hold-from"
        placeholder={today ?? 'from'}
        value={from}
        onChange={(e) => { setFrom(e.target.value); hold.reset(); }}
      />
      <input
        data-testid="person-hold-to"
        placeholder="to (open)"
        value={to}
        onChange={(e) => { setTo(e.target.value); hold.reset(); }}
      />
      <button type="button" data-testid="person-hold-toggle" disabled={hold.isPending} onClick={() => { rowAction.take(); submit({ from, to }); }}>
        Hold
      </button>
      {hold.isError && rowAction.isLatest && <span data-testid="person-error">The hold's dates must be calendar dates, the to-date not before the from-date.</span>}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 45, Component: HoldHeader }),
  PersonCells.fill({ order: 45, Component: PersonHold }),
];
