import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson, RefusedError } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { peopleQuery } from '../people/peopleQuery';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

/**
 * Move the claim to another person on the sheet. Only a steward moves a DRAFT, OPEN or PENDING claim; the server
 * refuses anything else, and the refusal shows on the claim (in its shared `claim-error`) until its next action.
 * The moved claim keeps its number and date, is live again until its new person's next statement, and lists first
 * under them.
 */
function ClaimReassign({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const people = (useQuery(peopleQuery(sheetId)).data ?? []).filter((p) => p.id !== claim.personId);
  const [to, setTo] = useState('');
  const [, setError] = useClaimError(claim.id);
  const once = useSubmitOnce();
  const reassign = useMutation({
    mutationFn: (personId: number) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/person`, { personId }, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The claim could not be moved.'),
  });

  return (
    <td>
      <select
        data-testid="claim-reassign-to"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          setError('');
        }}
      >
        <option value="">Move to…</option>
        {people.map((p) => (
          <option key={p.id} value={p.id}>
            {p.name}
          </option>
        ))}
      </select>
      <button
        type="button"
        data-testid="claim-reassign"
        disabled={reassign.isPending}
        onClick={() => {
          setError('');
          if (!to) {
            setError('Choose a person to move the claim to.');
            return;
          }
          once(() => reassign.mutateAsync(Number(to)));
        }}
      >
        Move
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 30, Component: ClaimReassign });
