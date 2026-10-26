import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

/**
 * The number of the claim this one was split off (blank for any other claim), and the entry and button that split an
 * amount off it into a new OPEN claim. The server refuses a claim that is not OPEN or is deferred, an amount not more
 * than 0.00 and less than the claim's, a role that may not open a claim, or a claim dated in a closed period, and the
 * refusal shows on the claim until its next action.
 */
function ClaimSplit({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = useClaimError(claim.id);
  const [amount, setAmount] = useState('');
  const once = useSubmitOnce();
  const split = useMutation({
    mutationFn: (value: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/split`, { amount: value }, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The claim could not be split.'),
  });

  return (
    <td>
      <span data-testid="claim-split-from">{claim.splitFrom ?? ''}</span>{' '}
      <input
        data-testid="claim-split-amount"
        placeholder="0.00"
        value={amount}
        onChange={(e) => {
          setAmount(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-split"
        disabled={split.isPending}
        onClick={() => {
          setError('');
          once(() => split.mutateAsync(amount.trim()));
        }}
      >
        Split
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 24, Component: ClaimSplit });
