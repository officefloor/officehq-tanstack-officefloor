import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

/**
 * Whether the claim's late charge is waived (yes/no) and the button that waives it. The server refuses anyone but a
 * steward, a claim not OPEN or PENDING, or one already waived, and the refusal shows on the claim until its next action.
 */
function ClaimWaive({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = useClaimError(claim.id);
  const once = useSubmitOnce();
  const waive = useMutation({
    mutationFn: () => putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/waive`, {}, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The late charge could not be waived.'),
  });

  return (
    <td>
      <span data-testid="claim-waived">{claim.waived ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="claim-waive"
        disabled={waive.isPending}
        onClick={() => {
          setError('');
          once(() => waive.mutateAsync());
        }}
      >
        Waive
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 27.5, Component: ClaimWaive });
