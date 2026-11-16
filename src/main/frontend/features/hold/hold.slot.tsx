import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { peopleKey, type Person } from '../people/people';
import { usePersonRowAction } from '../people/rowAction';
import { useShownColumns } from '../columns/columns';

function HoldHead({ user }: { user: string }) {
  const shown = useShownColumns(user);
  return (
    <>
      {shown('hold') && <th>On hold</th>}
      <th />
    </>
  );
}

/** Whether the person has any hold, their holds (one line each), and the dates and button that add one: from the
 *  from-date (blank: the sheet's today) until the to-date (blank: open-ended). Only their OPEN or PENDING claims dated
 *  within a hold, and not bound by a statement, count in no live total. A refused hold shows the row's error. */
function PersonHold({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const shown = useShownColumns(user);
  const queryClient = useQueryClient();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const submit = useSubmitOnce();
  const rowAction = usePersonRowAction(sheetId, person.id, 'hold');
  const hold = useMutation({
    mutationFn: (dates: { from: string; to: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/hold`, { ...dates, by: user }),
    onSuccess: () => {
      setFrom('');
      setTo('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <>
      {shown('hold') && <td data-testid="person-hold">{person.held ? 'yes' : 'no'}</td>}
      <td>
        {(person.holds ?? []).map((h, i) => (
          <div key={i} data-testid="person-hold-line">
            {h.from} – {h.to ?? ''}
          </div>
        ))}
        <input
          data-testid="person-hold-from"
          placeholder="from yyyy-MM-dd"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            hold.reset();
          }}
        />{' '}
        <input
          data-testid="person-hold-to"
          placeholder="to yyyy-MM-dd"
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
            if (!submit.begin()) return;
            rowAction.take();
            hold.reset();
            hold.mutate({ from: from.trim(), to: to.trim() });
          }}
        >
          Hold
        </button>
        {hold.isError && rowAction.isLatest && <span data-testid="person-error"> Could not hold this person.</span>}
      </td>
    </>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 50, Component: HoldHead }),
  PersonRow.fill({ order: 50, Component: PersonHold }),
];
