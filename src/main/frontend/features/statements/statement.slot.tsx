import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { claimsKey } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { useUser } from '../session/useUser';
import { usePersonError } from '../people/usePersonError';
import { statementsKey, statementsQuery } from './statementsQuery';
import type { Statements } from './statementsQuery';

/**
 * A person's statements and the button that takes one: it records their due-total now and binds each of
 * their counting claims to its figure now, until their next statement; and the button that cancels the last
 * one (a steward only), leaving the claims it bound live again. A date beside the button takes the statement as
 * of that date (on or before the sheet's today) instead: the figures as they stood at the end of it; blank takes it now.
 */
function PersonStatement({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = usePersonError(personId);
  const once = useSubmitOnce();
  const [asOf, setAsOf] = useState('');
  const shown = (useQuery(statementsQuery(sheetId)).data ?? []).find((s) => s.personId === personId);
  const take = useMutation({
    mutationFn: (date: string) =>
      postJson<Statements>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/statement`,
        { asOf: date.trim() },
        user,
      ),
    // The claims are re-read first, so the count moves on only once the bound figures show.
    onSuccess: async () => {
      setAsOf('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      await queryClient.invalidateQueries({ queryKey: statementsKey(sheetId) });
    },
    onError: () => setError('The statement could not be taken.'),
  });
  const cancel = useMutation({
    mutationFn: () =>
      putJson<Statements>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/unstate`, {}, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      await queryClient.invalidateQueries({ queryKey: statementsKey(sheetId) });
    },
    onError: () => setError('The statement could not be cancelled.'),
  });

  return (
    <td>
      <span data-testid="person-statement-count">{shown?.count ?? 0}</span>{' '}
      <span data-testid="person-stated-total">{shown?.dueTotal ?? ''}</span>{' '}
      <span data-testid="person-statement-date">{shown?.date ?? ''}</span>{' '}
      <input
        data-testid="person-statement-asof"
        type="date"
        title="As of"
        value={asOf}
        onChange={(e) => {
          setError('');
          setAsOf(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-statement"
        disabled={take.isPending}
        onClick={() => {
          setError('');
          once(() => take.mutateAsync(asOf));
        }}
      >
        Statement
      </button>{' '}
      <button
        type="button"
        data-testid="person-unstate"
        disabled={cancel.isPending}
        onClick={() => {
          setError('');
          once(() => cancel.mutateAsync());
        }}
      >
        Cancel statement
      </button>
    </td>
  );
}

/** Whether the person's last statement bound the claim to its figure. */
function ClaimStated({ claim }: { sheetId: string; claim: Claim }) {
  return <td data-testid="claim-stated">{claim.stated ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PersonHeaders.fill({ order: 17, Component: () => <th>Statements</th> }),
  PersonCells.fill({ order: 17, Component: PersonStatement }),
  ClaimCells.fill({ order: 15, Component: ClaimStated }),
];
