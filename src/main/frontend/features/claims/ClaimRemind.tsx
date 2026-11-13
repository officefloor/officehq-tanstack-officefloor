import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Claim } from './claims';
import { claimsKey } from './claims';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useClaimError } from './claimError';

// The last day a claim was reminded (blank when never) and the Remind button: an OPEN or PENDING claim is reminded
// once per sheet-today; the server refuses any other, and the refusal shows as the claim's error.
export function ClaimRemind({ claim }: { claim: Claim }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = useClaimError(claim.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const remind = useMutation({
    mutationFn: () =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/reminder?user=${encodeURIComponent(user)}`, {}),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () =>
      setError(`This ${claim.status} claim cannot be reminded (only an OPEN or PENDING claim, once per day, by a role allowed to change the sheet).`),
  });

  return (
    <>
      Reminded: <span data-testid="claim-reminded">{claim.reminded ?? ''}</span>{' '}
      <button
        type="button"
        data-testid="claim-remind"
        disabled={remind.isPending}
        onClick={() => {
          setError('');
          once(() => remind.mutateAsync());
        }}
      >
        Remind
      </button>
    </>
  );
}
