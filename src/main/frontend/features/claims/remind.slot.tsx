import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

/**
 * The last day the claim was reminded (empty when never) and the button that reminds it on the sheet's today. The
 * server refuses a claim not OPEN or PENDING, or one already reminded today, and the refusal shows on the claim until
 * its next action.
 */
function ClaimRemind({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = useClaimError(claim.id);
  const once = useSubmitOnce();
  const remind = useMutation({
    mutationFn: () => postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/remind`, {}, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The claim could not be reminded.'),
  });

  return (
    <td>
      <span data-testid="claim-reminded">{claim.remindedOn ?? ''}</span>{' '}
      <button
        type="button"
        data-testid="claim-remind"
        disabled={remind.isPending}
        onClick={() => {
          setError('');
          once(() => remind.mutateAsync());
        }}
      >
        Remind
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 27, Component: ClaimRemind });
