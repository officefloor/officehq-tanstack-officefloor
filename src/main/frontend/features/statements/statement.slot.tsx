import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { claimsKey } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { useUser } from '../session/useUser';
import { statementsKey, statementsQuery } from './statementsQuery';
import type { Statements } from './statementsQuery';

/**
 * A person's statements and the button that takes one: it records their due-total now and binds each of
 * their counting claims to its figure now, until their next statement.
 */
function PersonStatement({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useState('');
  const once = useSubmitOnce();
  const shown = (useQuery(statementsQuery(sheetId)).data ?? []).find((s) => s.personId === personId);
  const take = useMutation({
    mutationFn: () =>
      postJson<Statements>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/statement`, {}, user),
    // The claims are re-read first, so the count moves on only once the bound figures show.
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      await queryClient.invalidateQueries({ queryKey: statementsKey(sheetId) });
    },
    onError: () => setError('The statement could not be taken.'),
  });

  return (
    <td>
      <span data-testid="person-statement-count">{shown?.count ?? 0}</span>{' '}
      <span data-testid="person-stated-total">{shown?.dueTotal ?? ''}</span>{' '}
      <span data-testid="person-statement-date">{shown?.date ?? ''}</span>{' '}
      <button
        type="button"
        data-testid="person-statement"
        disabled={take.isPending}
        onClick={() => {
          setError('');
          once(() => take.mutateAsync());
        }}
      >
        Statement
      </button>
      {error && <span data-testid="person-error">{error}</span>}
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
