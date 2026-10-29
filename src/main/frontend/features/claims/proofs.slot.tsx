import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useUser } from '../session/useUser';
import { PROOF_KINDS } from '../settings/settingsQuery';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

/**
 * The claim's proofs count, with a count per kind, and the button that raises it by one, a proof of the kind chosen
 * (a receipt when none is). The server refuses a SETTLED or VOID claim, or a claim dated in a closed period, and the
 * refusal shows on the claim until its next action. A claim whose due is above the proof threshold in force on its
 * date is settled or approved only once it has a proof — one of each of the sheet's proof kinds, while it has them.
 */
function ClaimProofs({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = useClaimError(claim.id);
  const [kind, setKind] = useState('');
  const once = useSubmitOnce();
  const prove = useMutation({
    mutationFn: (chosen: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/proof`, chosen === '' ? {} : { kind: chosen }, user),
    onSuccess: () => {
      setKind('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The proof could not be added.'),
  });

  return (
    <td>
      <span data-testid="claim-proofs">{claim.proofs}</span> (receipts{' '}
      <span data-testid="claim-proofs-receipt">{claim.proofsReceipt}</span>, photos{' '}
      <span data-testid="claim-proofs-photo">{claim.proofsPhoto}</span>){' '}
      <select
        data-testid="claim-proof-kind"
        value={kind}
        onChange={(e) => {
          setKind(e.target.value);
          setError('');
        }}
      >
        <option value="">kind…</option>
        {PROOF_KINDS.map((k) => (
          <option key={k} value={k}>
            {k}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="claim-proof"
        disabled={prove.isPending}
        onClick={() => {
          setError('');
          once(() => prove.mutateAsync(kind));
        }}
      >
        Add proof
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 23, Component: ClaimProofs });
