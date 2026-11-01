import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useIsLatest } from '../../query/useIsLatest';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peoplePath, personActionKey } from '../people/people';
import type { Person } from '../people/people';
import { claimsKey } from '../claims/claims';
import { statementsKey, useStatements } from './statements';
import type { StatementSummary } from './statements';

function StatementHeader() {
  return (
    <>
      <th>Statements</th>
      <th>Stated total</th>
      <th>Stated on</th>
      <th />
    </>
  );
}

// A person's statements: how many, the last one's due-total and date, the button that takes the next and the one
// that cancels the last. Taking one binds their counting claims to their figures now, and cancelling one makes the
// claims it bound live again, so the claims are re-read before the count moves on. A date beside the button takes
// the statement as of that date (blank: now); it is cleared once the statement is taken or refused, so a refused date
// never carries into the next press, and changing it clears the statement's error.
function PersonStatements({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: all = [] } = useStatements(sheet);
  const mine = all.find((s) => s.person === person.id);
  const [asof, setAsof] = useState('');
  const take = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (date: string) =>
      postJson<StatementSummary[]>(byUser(`${peoplePath(sheet)}/${person.id}/statement`, user), { asof: date }),
    onSuccess: async (updated) => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      queryClient.setQueryData(statementsKey(sheet), updated);
    },
    onSettled: () => setAsof(''),
  });
  const cancel = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: () =>
      postJson<StatementSummary[]>(byUser(`${peoplePath(sheet)}/${person.id}/unstate`, user), {}),
    onSuccess: async (updated) => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      queryClient.setQueryData(statementsKey(sheet), updated);
    },
  });
  const act = useSubmitOnce(take);
  const actCancel = useSubmitOnce(cancel);
  const latest = useIsLatest(personActionKey(sheet, person.id), take.submittedAt);
  const latestCancel = useIsLatest(personActionKey(sheet, person.id), cancel.submittedAt);

  return (
    <>
      <td data-testid="person-statement-count">{mine?.count ?? 0}</td>
      <td data-testid="person-stated-total">{mine?.lastDueTotal ?? ''}</td>
      <td data-testid="person-statement-date">{mine?.lastDate ?? ''}</td>
      <td>
        <input
          type="date"
          data-testid="person-statement-asof"
          aria-label="Statement as of"
          value={asof}
          onChange={(e) => {
            setAsof(e.target.value);
            take.reset();
          }}
        />{' '}
        <button type="button" data-testid="person-statement" disabled={take.isPending} onClick={() => act(asof)}>
          Statement
        </button>{' '}
        <button type="button" data-testid="person-unstate" disabled={cancel.isPending} onClick={() => actCancel()}>
          Cancel statement
        </button>
        {take.isError && latest && (
          <span data-testid="person-error" role="alert">
            {' '}The statement could not be taken.
          </span>
        )}
        {cancel.isError && latestCancel && !(take.isError && latest) && (
          <span data-testid="person-error" role="alert">
            {' '}The statement could not be cancelled.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 20, Component: StatementHeader }),
  PersonColumns.fill({ order: 20, Component: PersonStatements }),
];
