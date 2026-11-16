import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { usePersonRowAction } from '../people/rowAction';
import { claimsKey } from '../claims/claims';
import { statementsKey, useStatements } from './statements';

function StatementsHead() {
  return (
    <>
      <th>Statements</th>
      <th>Stated total</th>
      <th>Stated on</th>
      <th />
    </>
  );
}

/** A person's statement count, their last statement's due-total and date, and the date (blank: now) and button that
 *  take one: it records their due-total as of that date and binds each of their claims that counted then to its rate
 *  figure then (not floored); and
 *  the button that cancels their last statement (a steward only), making the claims it bound live again. */
function PersonStatements({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const statements = useStatements(sheetId);
  const queryClient = useQueryClient();
  const mine = statements.data?.find((s) => s.personId === person.id);
  const [asOf, setAsOf] = useState('');
  const rowAction = usePersonRowAction(sheetId, person.id, 'statement');
  const take = useMutation({
    mutationFn: (date: string) =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/statements`, { by: user, asOf: date }),
    onSuccess: () => {
      setAsOf('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: statementsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });

  const cancelAction = usePersonRowAction(sheetId, person.id, 'unstate');
  const cancel = useMutation({
    mutationFn: () =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/statements/cancel`, { by: user }),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: statementsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]),
  });

  return (
    <>
      <td data-testid="person-statement-count">{mine?.count ?? 0}</td>
      <td data-testid="person-stated-total">{mine?.dueTotal ?? ''}</td>
      <td data-testid="person-statement-date">{mine?.date ?? ''}</td>
      <td>
        <input
          data-testid="person-statement-asof"
          placeholder="as of yyyy-MM-dd"
          value={asOf}
          onChange={(e) => {
            setAsOf(e.target.value);
            take.reset();
          }}
        />{' '}
        <button
          type="button"
          data-testid="person-statement"
          disabled={take.isPending}
          onClick={() => {
            rowAction.take();
            take.reset();
            take.mutate(asOf.trim());
          }}
        >
          Statement
        </button>
        {take.isError && rowAction.isLatest && <span data-testid="person-error"> Could not take a statement for this person.</span>}
        <button
          type="button"
          data-testid="person-unstate"
          disabled={cancel.isPending}
          onClick={() => {
            cancelAction.take();
            cancel.reset();
            cancel.mutate();
          }}
        >
          Cancel statement
        </button>
        {cancel.isError && cancelAction.isLatest && <span data-testid="person-error"> Could not cancel this person's statement.</span>}
      </td>
    </>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 30, Component: StatementsHead }),
  PersonRow.fill({ order: 30, Component: PersonStatements }),
];
