import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Claim } from './claims';
import { claimsKey } from './claims';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useClaimError } from './claimError';

// The user the claim's review is assigned to (blank when none) and the box to assign it: only a steward (by delegation
// too) assigns a REVIEW claim's review; the server refuses a blank user, any other status or anyone else, and the
// refusal shows as the claim's error.
export function ClaimReviewAssign({ claim }: { claim: Claim }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = useClaimError(claim.id);
  const [assignee, setAssignee] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const assign = useMutation({
    mutationFn: (body: { user: string }) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/review-assignee?user=${encodeURIComponent(user)}`, body),
    onSuccess: async () => {
      setAssignee('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () => setError('This review cannot be assigned (only a steward assigns a REVIEW claim to a user).'),
  });

  return (
    <>
      Reviewer: <span data-testid="claim-review-assigned">{claim.reviewAssigned ?? ''}</span>{' '}
      <input
        data-testid="claim-review-user"
        placeholder="Reviewer"
        value={assignee}
        onChange={(e) => {
          setError('');
          setAssignee(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-review-assign"
        disabled={assign.isPending}
        onClick={() => {
          setError('');
          if (assignee.trim() === '') {
            setError('Type the user to assign the review to.');
            return;
          }
          once(() => assign.mutateAsync({ user: assignee }));
        }}
      >
        Assign review
      </button>
    </>
  );
}
