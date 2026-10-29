import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useUser } from '../session/useUser';
import { useSheetToday } from '../sheet/useSheetToday';
import { claimsKey, isDeferred } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

/** A voided claim keeps the deferral it had: it still shows as deferred until the date it was deferred to. */
const deferralKept = (claim: Claim, today: string) =>
  claim.status === 'VOID' && claim.deferredUntil !== null && (today === '' || today < claim.deferredUntil);

/**
 * Whether the claim is deferred on the sheet's today, the date it was deferred to, and the entry and button that
 * defer it. The server refuses a claim that is not OPEN, a date not after the sheet's today, or a claim dated in a
 * closed period, and the refusal shows on the claim until its next action.
 */
function ClaimDefer({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const today = useSheetToday(sheetId);
  const [user] = useUser();
  const [error, setError] = useClaimError(claim.id);
  const [until, setUntil] = useState('');
  const once = useSubmitOnce();
  const defer = useMutation({
    mutationFn: (date: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/defer`, { until: date }, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The claim could not be deferred.'),
  });

  return (
    <td>
      <span data-testid="claim-deferred">{isDeferred(claim, today) || deferralKept(claim, today) ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-deferred-until">{claim.deferredUntil ?? ''}</span>{' '}
      <input
        data-testid="claim-defer-date"
        placeholder="yyyy-mm-dd"
        value={until}
        onChange={(e) => {
          setUntil(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-defer"
        disabled={defer.isPending}
        onClick={() => {
          setError('');
          once(() => defer.mutateAsync(until.trim()));
        }}
      >
        Defer
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 22, Component: ClaimDefer });
