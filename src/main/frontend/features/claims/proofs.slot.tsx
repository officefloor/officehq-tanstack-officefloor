import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

/**
 * The claim's proofs count and the button that raises it by one. The server refuses a SETTLED or VOID claim, or a
 * claim dated in a closed period, and the refusal shows on the claim until its next action. A claim whose due is
 * above the proof threshold in force on its date is settled or approved only once it has a proof.
 */
function ClaimProofs({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = useClaimError(claim.id);
  const once = useSubmitOnce();
  const prove = useMutation({
    mutationFn: () => postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/proof`, {}, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The proof could not be added.'),
  });

  return (
    <td>
      <span data-testid="claim-proofs">{claim.proofs}</span>{' '}
      <button
        type="button"
        data-testid="claim-proof"
        disabled={prove.isPending}
        onClick={() => {
          setError('');
          once(() => prove.mutateAsync());
        }}
      >
        Add proof
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 23, Component: ClaimProofs });
