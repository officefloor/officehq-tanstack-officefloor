import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson, RefusedError } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import { useClaimError } from './useClaimError';
import type { Claim, ClaimStatus } from './claimsQuery';

type Decision = 'approve' | 'reject';

const ACTIONS: Array<{ action: string; to: ClaimStatus; label: string; decision?: Decision }> = [
  { action: 'open', to: 'OPEN', label: 'Open' },
  { action: 'settle', to: 'SETTLED', label: 'Settle' },
  { action: 'void', to: 'VOID', label: 'Void' },
  { action: 'approve', to: 'SETTLED', label: 'Approve', decision: 'approve' },
];

/**
 * A claim's status and the buttons that change it. Every button is offered; the server refuses a
 * change the status does not allow, a settle by the user who opened the claim, a clerk's settle of a claim
 * whose due is above the sheet's clerk limit, or any change to a claim dated in a closed period, and the refusal
 * shows on the claim until its next action. A clerk's settle above the clerk limit makes the claim PENDING: a
 * steward other than its opener approves it (SETTLED) or rejects it with a reason (back to OPEN).
 */
function ClaimStatusCell({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useClaimError(claim.id);
  const [reason, setReason] = useState('');
  const once = useSubmitOnce();
  const change = useMutation({
    mutationFn: ({ to, decision, reason }: { to: ClaimStatus; decision?: Decision; reason?: string }) =>
      putJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/status/${to}`,
        { action: decision, reason },
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e, { to, decision, reason }) =>
      setError(
        e instanceof RefusedError
          ? e.message
          : (decision !== undefined) !== (claim.status === 'PENDING') && to !== 'VOID'
            ? decision !== undefined
              ? `Only a PENDING claim can be ${decision === 'approve' ? 'approved' : 'rejected'}.`
              : 'A PENDING claim is approved or rejected by a steward, or voided.'
            : to === 'SETTLED' && claim.openedBy === user
              ? `The user who opened a claim cannot ${decision === 'approve' ? 'approve' : 'settle'} it.`
              : decision === 'reject' && !reason?.trim()
                ? 'Give a reason to reject the claim.'
                : decision !== undefined
                  ? 'Only a steward can approve or reject a pending claim.'
                  : `A ${claim.status} claim cannot become ${to}.`,
      ),
  });

  return (
    <td>
      <span data-testid="claim-status">{claim.status}</span>{' '}
      {ACTIONS.map(({ action, to, label, decision }) => (
        <button
          key={action}
          type="button"
          data-testid={`claim-${action}`}
          disabled={change.isPending}
          onClick={() => {
            setError('');
            once(() => change.mutateAsync({ to, decision }));
          }}
        >
          {label}
        </button>
      ))}
      <input
        data-testid="claim-reject-reason"
        placeholder="Reason"
        value={reason}
        onChange={(e) => {
          setReason(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-reject"
        disabled={change.isPending}
        onClick={() => {
          setError('');
          once(() => change.mutateAsync({ to: 'OPEN', decision: 'reject', reason }).then(() => setReason('')));
        }}
      >
        Reject
      </button>
      {error && <span data-testid="claim-error">{error}</span>}
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 20, Component: ClaimStatusCell });
