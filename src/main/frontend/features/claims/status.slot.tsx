import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson, RefusedError } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useUser } from '../session/useUser';
import { voidReasonsQuery } from '../voidReasons/voidReasonsQuery';
import { claimsKey } from './claimsQuery';
import { useClaimError } from './useClaimError';
import type { Claim, ClaimStatus } from './claimsQuery';

type Decision = 'approve' | 'reject' | 'review';

const ACTIONS: Array<{ action: string; to: ClaimStatus; label: string; decision?: Decision }> = [
  { action: 'open', to: 'OPEN', label: 'Open' },
  { action: 'settle', to: 'SETTLED', label: 'Settle' },
  { action: 'void', to: 'VOID', label: 'Void' },
  { action: 'approve', to: 'SETTLED', label: 'Approve', decision: 'approve' },
  { action: 'review', to: 'OPEN', label: 'Review', decision: 'review' },
  { action: 'delete', to: 'DELETED', label: 'Delete' },
  { action: 'restore', to: 'DRAFT', label: 'Restore' },
];

/**
 * A claim's status and the buttons that change it. Every button is offered; the server refuses a
 * change the status does not allow, a settle by the user who opened the claim, a clerk's settle of a claim
 * whose due is above the sheet's clerk limit, or any change to a claim due by a date in a closed period, and the refusal
 * shows on the claim until its next action. A clerk's settle above the clerk limit makes the claim PENDING: a
 * steward other than its opener approves it (SETTLED) or rejects it with a reason (back to OPEN). A steward can
 * re-settle a SETTLED claim, freezing it again at today's figure; the server refuses anyone else or any other status.
 * A steward can also void a SETTLED claim within 7 days of its settlement; the server refuses anyone else or a later day.
 * While the sheet has void reasons, a void needs one chosen from them; the server refuses a void without one.
 * A DRAFT claim can be deleted (DELETED counts nowhere and cannot be edited, opened or voided), and restored to DRAFT
 * on the sheet's today it was deleted; the server refuses a restore on a later day.
 * A settle can be given a date (on or before the sheet's today): the claim is frozen at its due as the as-of view shows
 * it at the end of that date; the server refuses a later date, or a claim not on the sheet then.
 * Opening a claim whose due is above the review threshold makes it REVIEW (unnumbered, in no total): a steward's review
 * makes it OPEN (numbered then); the server refuses anyone else. A REVIEW claim can otherwise only be voided.
 */
function ClaimStatusCell({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useClaimError(claim.id);
  const [reason, setReason] = useState('');
  const [voidReason, setVoidReason] = useState('');
  const [settleOn, setSettleOn] = useState('');
  const voidReasons = useQuery(voidReasonsQuery(sheetId)).data ?? [];
  const once = useSubmitOnce();
  const change = useMutation({
    mutationFn: ({ to, decision, reason, voidReason, settleOn }: { to: ClaimStatus; decision?: Decision; reason?: string; voidReason?: string; settleOn?: string }) =>
      putJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/status/${to}`,
        { action: decision, reason, voidReason, settleOn },
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e, { to, decision, reason }) =>
      setError(
        e instanceof RefusedError
          ? e.message
          : decision === 'review'
            ? claim.status === 'REVIEW' ? 'Only a steward can review a claim.' : 'Only a REVIEW claim is reviewed.'
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

  const resettle = useMutation({
    mutationFn: () =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/resettle`, {}, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) =>
      setError(e instanceof RefusedError ? e.message : 'The claim could not be re-settled.'),
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
            once(() =>
              to === 'VOID'
                ? change.mutateAsync({ to, voidReason }).then(() => setVoidReason(''))
                : action === 'settle'
                  ? change.mutateAsync({ to, settleOn: settleOn.trim() || undefined }).finally(() => setSettleOn(''))
                  : change.mutateAsync({ to, decision }),
            );
          }}
        >
          {label}
        </button>
      ))}
      <input
        type="date"
        data-testid="claim-settle-date"
        aria-label="Settle as of"
        value={settleOn}
        onChange={(e) => {
          setSettleOn(e.target.value);
          setError('');
        }}
      />{' '}
      {voidReasons.length > 0 && (
        <select
          data-testid="claim-void-reason"
          value={voidReason}
          onChange={(e) => {
            setVoidReason(e.target.value);
            setError('');
          }}
        >
          <option value="">Void reason…</option>
          {voidReasons.map((r) => (
            <option key={r.reason} value={r.reason}>
              {r.reason}
            </option>
          ))}
        </select>
      )}{' '}
      <button
        type="button"
        data-testid="claim-resettle"
        disabled={resettle.isPending}
        onClick={() => {
          setError('');
          once(() => resettle.mutateAsync());
        }}
      >
        Re-settle
      </button>
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
