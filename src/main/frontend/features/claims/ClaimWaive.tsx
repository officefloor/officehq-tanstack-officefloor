import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Claim } from './claims';
import { claimsKey } from './claims';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useClaimError } from './claimError';

// Whether the claim's late charge is waived (yes/no) and the Waive button: a steward waives an OPEN or PENDING claim's
// late charge once; the server refuses anyone else, any other status and a claim already waived, and the refusal
// shows as the claim's error.
export function ClaimWaive({ claim }: { claim: Claim }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = useClaimError(claim.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const waive = useMutation({
    mutationFn: () =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/waiver?user=${encodeURIComponent(user)}`, {}),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () =>
      setError(`This claim's late charge cannot be waived (only a steward, only on an OPEN or PENDING claim, only once).`),
  });

  return (
    <>
      Waived: <span data-testid="claim-waived">{claim.waived ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="claim-waive"
        disabled={waive.isPending}
        onClick={() => {
          setError('');
          once(() => waive.mutateAsync());
        }}
      >
        Waive
      </button>
    </>
  );
}
