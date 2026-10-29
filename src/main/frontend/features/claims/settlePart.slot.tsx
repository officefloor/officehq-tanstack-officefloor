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
 * The entry and button that part-settle an OPEN claim: the amount is split off as the split button does, and the new
 * claim settled by the user as the settle button does (PENDING above the clerk limit). Four eyes and the proof rule are
 * judged on this claim. The server refuses the whole action when the split or the settle would be refused, and the
 * refusal shows on the claim until its next action.
 */
function ClaimSettlePart({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = useClaimError(claim.id);
  const [amount, setAmount] = useState('');
  const once = useSubmitOnce();
  const settlePart = useMutation({
    mutationFn: (value: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/settle-part`, { amount: value }, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The claim could not be part-settled.'),
  });

  return (
    <td>
      <input
        data-testid="claim-settle-part-amount"
        placeholder="0.00"
        value={amount}
        onChange={(e) => {
          setAmount(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-settle-part"
        disabled={settlePart.isPending}
        onClick={() => {
          setError('');
          once(() => settlePart.mutateAsync(amount.trim()));
        }}
      >
        Settle part
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 24.5, Component: ClaimSettlePart });
