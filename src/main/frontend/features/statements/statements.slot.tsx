import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { claimsKey } from '../claims/useClaims';
import { peopleKey, usePeople, type Person } from '../people/people.slot';
import { useUser } from '../session/useSession';
import { personActionKey, useLastPersonAction } from '../people/personAction';

function StatementHeads() {
  return (
    <>
      <th>Statements</th>
      <th>Last stated total</th>
      <th>Last statement</th>
      <th />
    </>
  );
}

// A person's statement count, the last statement's due-total and date, the button that takes a statement and the one that cancels the last:
// it records their due-total and binds their counting claims to the figures they have now — or, with a date in the
// as-of field (on or before the sheet's today), the figures they had at the end of that date. A refusal shows on
// the person until the next action on their row.
function StatementCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const person = usePeople(sheetId).data?.find((p) => p.id === personId);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const [asOf, setAsOf] = useState('');
  const take = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: (date: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/statements`, {
        by: user ?? '',
        asOf: date.trim(),
      }),
    onSuccess: async () => {
      setAsOf('');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });
  const lastTake = useLastPersonAction(sheetId, personId, take.submittedAt);
  // Cancels the last statement (a steward only): the claims it bound are live again.
  const unstate = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/unstate`, { by: user ?? '' }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });
  const lastUnstate = useLastPersonAction(sheetId, personId, unstate.submittedAt);

  return (
    <>
      <td data-testid="person-statement-count">{person?.statementCount ?? 0}</td>
      <td data-testid="person-stated-total">{person?.statedTotal ?? ''}</td>
      <td data-testid="person-statement-date">{person?.statementDate ?? ''}</td>
      <td>
        <input
          type="date"
          data-testid="person-statement-asof"
          value={asOf}
          onChange={(e) => {
            setAsOf(e.target.value);
            take.reset();
          }}
        />{' '}
        <button type="button" data-testid="person-statement" disabled={take.isPending} onClick={() => take.mutate(asOf)}>
          Statement
        </button>
        <button
          type="button"
          data-testid="person-unstate"
          disabled={unstate.isPending}
          onClick={() => unstate.mutate()}
        >
          Cancel statement
        </button>
        {take.isError && lastTake && (
          <span data-testid="person-error" role="alert">
            {take.error.message}
          </span>
        )}
        {unstate.isError && lastUnstate && (
          <span data-testid="person-error" role="alert">
            {unstate.error.message}
          </span>
        )}
      </td>
    </>
  );
}

// Whether the person's last statement bound the claim to its figure.
function ClaimStated({ claim }: { sheetId: string; user: string; personId: number; claim: { stated: boolean } }) {
  return <span data-testid="claim-stated">{claim.stated ? 'yes' : 'no'}</span>;
}

export const contributions = [
  PersonHeads.fill({ order: 30, Component: StatementHeads }),
  PersonCells.fill({ order: 30, Component: StatementCells }),
  ClaimCells.fill({ order: 15, Component: ClaimStated }),
];
