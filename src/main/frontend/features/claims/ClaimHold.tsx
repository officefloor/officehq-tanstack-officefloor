import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Claim } from './claims';
import { claimsKey } from './claims';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useClaimError } from './claimError';

// Whether the claim is held (yes/no) and the Hold/Release toggle: an OPEN or PENDING claim is held or released; the
// server refuses any other status, and the refusal shows as the claim's error.
export function ClaimHold({ claim }: { claim: Claim }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = useClaimError(claim.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const toggle = useMutation({
    mutationFn: () =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/hold?user=${encodeURIComponent(user)}`, {}),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () => setError(`This claim cannot be ${claim.held ? 'released' : 'held'} (only an OPEN or PENDING claim).`),
  });

  return (
    <>
      Held: <span data-testid="claim-held">{claim.held ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="claim-hold"
        disabled={toggle.isPending}
        onClick={() => {
          setError('');
          once(() => toggle.mutateAsync());
        }}
      >
        {claim.held ? 'Release' : 'Hold'}
      </button>
    </>
  );
}
