import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { claimsKey } from '../claims/useClaims';
import { peopleKey, type Person } from '../people/usePeople';
import { useRowAction } from '../people/useRowAction';
import { Col } from '../columns/useColumns';

/**
 * A person's statements: the button to take one, which records their due-total now and binds each
 * of their counting claims to the figure it has now (the rest are unbound), how many have been
 * taken, and the last one's due-total and date (blank when none has been taken). A date beside the
 * button takes it as of that date (on or before the sheet's today): the claims that counted at the
 * end of it are bound to their figures as they stood then, and the statement is dated it. A steward can
 * cancel the last statement: the claims it bound become live again, and the row shows the one
 * before it (or nothing).
 */
function PersonStatement({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const [asOf, setAsOf] = useState('');
  const take = useMutation({
    mutationFn: (date: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/statement`, { user, asOf: date }),
    // Taken or refused, the date is spent: the next press takes it as of a date only when one is typed again.
    onSettled: () => {
      setAsOf('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]);
    },
  });
  const cancel = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/unstate`, { user }),
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]),
  });
  const actionTaken = useRowAction(sheet, person.id, () => {
    take.reset();
    cancel.reset();
  });
  const taken = person.statementCount > 0;
  return (
    <>
      <Col user={user} id="statement-count"><td data-testid="person-statement-count">{person.statementCount}</td></Col>
      <Col user={user} id="stated-total"><td data-testid="person-stated-total">{taken ? (person.statedTotal ?? 'n/a') : ''}</td></Col>
      <Col user={user} id="statement-date"><td data-testid="person-statement-date">{taken ? (person.statementDate ?? '') : ''}</td></Col>
      <td>
        <input
          type="text"
          placeholder="as of yyyy-mm-dd"
          data-testid="person-statement-asof"
          value={asOf}
          onChange={(e) => {
            setAsOf(e.target.value);
            take.reset();
          }}
        />{' '}
        <button type="button" data-testid="person-statement" disabled={take.isPending} onClick={() => {
            actionTaken();
            cancel.reset();
            take.mutate(asOf);
          }}>
          Statement
        </button>
        <button type="button" data-testid="person-unstate" disabled={cancel.isPending} onClick={() => {
            actionTaken();
            take.reset();
            cancel.mutate();
          }}>
          Cancel statement
        </button>
        {take.isError && (
          <span data-testid="person-error" role="alert">
            {' '}
            Could not take this statement.
          </span>
        )}
        {cancel.isError && (
          <span data-testid="person-error" role="alert">
            {' '}
            Could not cancel the statement.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 20,
    Component: ({ user }: { user: string; sheet: string }) => (
      <>
        <Col user={user} id="statement-count"><th>Statements</th></Col>
        <Col user={user} id="stated-total"><th>Last stated</th></Col>
        <Col user={user} id="statement-date"><th>Stated on</th></Col>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 20, Component: PersonStatement }),
];
