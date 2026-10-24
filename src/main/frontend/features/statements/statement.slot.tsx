import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { claimsQueryKey } from '../claims/claims';
import { peoplePath, peopleQueryKey, type Person } from '../people/people';
import { useColumnShown } from '../people/columns';

// A person's statements: one line per statement still standing, oldest first, as <date> <due-total>; how many were taken, the last one's due-total and date, and the button that takes
// one — recording their due-total now and binding each of their counted claims to the figure it has now; or, with a
// date in the entry beside it (on or before the sheet's today), as of the end of that date — and the
// button a steward cancels the last one with, making the claims it bound live again (anyone else, or a person with no
// statement, shows the row's error).
function StatementHeader() {
  return <th>Statements</th>;
}

function PersonStatement({ person }: { person: Person }) {
  const countShown = useColumnShown('statement-count');
  const totalShown = useColumnShown('stated-total');
  const dateShown = useColumnShown('statement-date');
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const today = useSheetToday();
  const [asOf, setAsOf] = useState('');
  const queryClient = useQueryClient();
  const take = useMutation({
    mutationFn: (date: string) =>
      putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/statement`
        + (date.trim() ? `?asOf=${encodeURIComponent(date.trim())}` : ''), undefined, user),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') }),
        queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet ?? '') }),
      ]);
    },
    // A date typed for one statement, taken or refused, never carries into the next.
    onSettled: () => setAsOf(''),
  });
  const submit = useSubmitOnce(take);
  const rowAction = useRowAction(`person-${person.id}`);
  const cancel = useMutation({
    mutationFn: () => putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/unstate`, undefined, user),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') }),
        queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet ?? '') }),
      ]),
  });
  const submitCancel = useSubmitOnce(cancel);
  const cancelAction = useRowAction(`person-${person.id}`);
  return (
    <td>
      {countShown && <><span data-testid="person-statement-count">{person.statementCount}</span>{' '}</>}
      {totalShown && <><span data-testid="person-stated-total">{person.statedTotal ?? ''}</span>{' '}</>}
      {dateShown && <><span data-testid="person-statement-date">{person.statementDate ?? ''}</span>{' '}</>}
      <div>
        {person.statements.map((s, i) => (
          <div key={i} data-testid="person-statement-line-line">{s.date} {s.dueTotal}</div>
        ))}
      </div>
      <input
        data-testid="person-statement-asof"
        placeholder={today ?? 'as of'}
        value={asOf}
        onChange={(e) => { setAsOf(e.target.value); take.reset(); }}
      />
      <button type="button" data-testid="person-statement" disabled={take.isPending} onClick={() => { rowAction.take(); submit(asOf); }}>
        Statement
      </button>
      <button type="button" data-testid="person-unstate" disabled={cancel.isPending} onClick={() => { cancelAction.take(); submitCancel(undefined); }}>
        Cancel statement
      </button>
      {take.isError && rowAction.isLatest && <span data-testid="person-error">The statement could not be taken.</span>}
      {cancel.isError && cancelAction.isLatest && <span data-testid="person-error">The statement could not be cancelled.</span>}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 30, Component: StatementHeader }),
  PersonCells.fill({ order: 30, Component: PersonStatement }),
];
