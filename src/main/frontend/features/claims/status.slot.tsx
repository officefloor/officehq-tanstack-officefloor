import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson, RefusedError } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useSubmitOnce } from '../../api/useSubmitOnce';
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
 * change the status does not allow, a settle by the user who opened the claim, a clerk's settle of a claim
 * whose due is above the sheet's clerk limit, or any change to a claim dated in a closed period, and the refusal
 * shows on the claim until its next action.
 */
function ClaimStatusCell({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useState('');
  const once = useSubmitOnce();
  const change = useMutation({
    mutationFn: (to: ClaimStatus) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/status/${to}`, {}, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e, to) =>
      setError(
        e instanceof RefusedError
          ? e.message
          : to === 'SETTLED' && claim.status === 'OPEN' && claim.openedBy === user
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
            once(() => change.mutateAsync(to));
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
