import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

/**
 * Whether the claim is held (yes/no) and the toggle that holds or releases it. The server refuses a claim not OPEN or
 * PENDING, and the refusal shows on the claim until its next action.
 */
function ClaimHold({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = useClaimError(claim.id);
  const once = useSubmitOnce();
  const hold = useMutation({
    mutationFn: () => putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/hold`, {}, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The claim could not be held or released.'),
  });

  return (
    <td>
      <span data-testid="claim-held">{claim.held ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="claim-hold"
        disabled={hold.isPending}
        onClick={() => {
          setError('');
          once(() => hold.mutateAsync());
        }}
      >
        {claim.held ? 'Release' : 'Hold'}
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 27.6, Component: ClaimHold });
