import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { useUser } from '../session/useSession';
import { claimsKey, type Claim, type ClaimStatus } from './useClaims';

const ACTIONS: Array<{ action: string; status: ClaimStatus; label: string }> = [
  { action: 'open', status: 'OPEN', label: 'Open' },
  { action: 'settle', status: 'SETTLED', label: 'Settle' },
  { action: 'void', status: 'VOID', label: 'Void' },
];

// A claim's status, and the buttons that open, settle or void it. A change its status does not
// allow is refused by the server, and the refusal shows on the claim until its next action.
function ClaimStatusCell({ sheetId, claim }: { sheetId: string; user: string; personId: number; claim: Claim }) {
  const [user] = useUser();
  const queryClient = useQueryClient();
  const change = useMutation({
    mutationFn: (status: ClaimStatus) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/status`, { status, by: user ?? '' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  return (
    <>
      <span data-testid="claim-status">{claim.status}</span>{' '}
      {ACTIONS.map((a) => (
        <button
          key={a.action}
          type="button"
          data-testid={`claim-${a.action}`}
          disabled={change.isPending}
          onClick={() => change.mutate(a.status)}
        >
          {a.label}
        </button>
      ))}{' '}
      {change.isError && (
        <span data-testid="claim-error" role="alert">
          {change.error.message}
        </span>
      )}
    </>
  );
}

export const contribution = ClaimCells.fill({ order: 20, Component: ClaimStatusCell });
