import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim, ClaimStatus } from './claimsQuery';

const ACTIONS: Array<{ action: string; to: ClaimStatus; label: string }> = [
  { action: 'open', to: 'OPEN', label: 'Open' },
  { action: 'settle', to: 'SETTLED', label: 'Settle' },
  { action: 'void', to: 'VOID', label: 'Void' },
];

/**
 * A claim's status and the buttons that change it. Every button is offered; the server refuses a
 * change the status does not allow, or a settle by the user who opened the claim, and the refusal
 * shows on the claim until its next action.
 */
function ClaimStatusCell({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useState('');
  const change = useMutation({
    mutationFn: (to: ClaimStatus) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/status/${to}`, {}, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (_e, to) =>
      setError(
        to === 'SETTLED' && claim.status === 'OPEN' && claim.openedBy === user
          ? 'The user who opened a claim cannot settle it.'
          : `A ${claim.status} claim cannot become ${to}.`,
      ),
  });

  return (
    <td>
      <span data-testid="claim-status">{claim.status}</span>{' '}
      {ACTIONS.map(({ action, to, label }) => (
        <button
          key={action}
          type="button"
          data-testid={`claim-${action}`}
          disabled={change.isPending}
          onClick={() => {
            setError('');
            change.mutate(to);
          }}
        >
          {label}
        </button>
      ))}
      {error && <span data-testid="claim-error">{error}</span>}
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 20, Component: ClaimStatusCell });
