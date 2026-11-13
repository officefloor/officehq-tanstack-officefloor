import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Claim } from './claims';
import { claimsKey } from './claims';
import { useOneAtATime } from '../../query/useOneAtATime';

type Action = 'open' | 'settle' | 'approve' | 'reject' | 'void';

const ACTIONS: Array<{ action: Action; label: string }> = [
  { action: 'open', label: 'Open' },
  { action: 'settle', label: 'Settle' },
  { action: 'approve', label: 'Approve' },
  { action: 'reject', label: 'Reject' },
  { action: 'void', label: 'Void' },
];

const REFUSED: Partial<Record<Action, string>> = {
  settle: ' (the user who opened it cannot settle it)',
  approve: ' (only a steward approves a pending claim, and not the user who opened it)',
  reject: ' (only a steward rejects a pending claim, and a reason is required)',
};

// A claim's status and the buttons that change it. Every button is shown; the server refuses a
// change the status does not allow, and the refusal shows as the claim's error. A clerk's settle
// above the clerk limit makes the claim PENDING; a reject sends the reason typed beside it.
export function ClaimStatus({ claim }: { claim: Claim }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [error, setError] = useState('');
  const [reason, setReason] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const change = useMutation({
    mutationFn: (action: Action) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/${action}?user=${encodeURIComponent(user)}`,
        action === 'reject' ? { reason } : {},
      ),
    onSuccess: async (_c, action) => {
      if (action === 'reject') setReason('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: (_e, action) => setError(`This ${claim.status} claim cannot be changed by ${action}${REFUSED[action] ?? ''}.`),
  });

  return (
    <>
      <span data-testid="claim-status">{claim.status}</span>{' '}
      {ACTIONS.map(({ action, label }) => (
        <button
          key={action}
          type="button"
          data-testid={`claim-${action}`}
          disabled={change.isPending}
          onClick={() => {
            setError('');
            once(() => change.mutateAsync(action));
          }}
        >
          {label}
        </button>
      ))}
      <input
        data-testid="claim-reject-reason"
        aria-label="Reject reason"
        placeholder="reason"
        value={reason}
        onChange={(e) => {
          setReason(e.target.value);
          setError('');
        }}
      />
      {error && <span data-testid="claim-error">{error}</span>}
    </>
  );
}
