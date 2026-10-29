import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

/**
 * The user the claim's review was last assigned to, and the entry and button that assign it. The server refuses anyone
 * but a steward, a claim not in REVIEW, or a blank user, and the refusal shows on the claim until its next action.
 */
function ClaimReviewAssign({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = useClaimError(claim.id);
  const [assignee, setAssignee] = useState('');
  const once = useSubmitOnce();
  const assign = useMutation({
    mutationFn: (to: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/review-assign`, { user: to }, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The review could not be assigned.'),
  });

  return (
    <td>
      <span data-testid="claim-review-assigned">{claim.reviewUser ?? ''}</span>{' '}
      <input
        data-testid="claim-review-user"
        placeholder="user"
        value={assignee}
        onChange={(e) => {
          setAssignee(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-review-assign"
        disabled={assign.isPending}
        onClick={() => {
          setError('');
          once(() => assign.mutateAsync(assignee));
        }}
      >
        Assign review
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 27.8, Component: ClaimReviewAssign });
