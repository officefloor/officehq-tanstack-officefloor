import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

/**
 * The entry and button with which a draft's amount is changed in its row, beside its edit button. It saves through the
 * claim form's edit, keeping the claim's date, note and the number it corrects, so the server refuses it by the same
 * rules (the amount's form, a claim that is not a draft, one another user has locked, a closed period, a viewer) and
 * writes CLAIM_EDITED; the refusal shows on the claim until its next action.
 */
function ClaimInlineAmount({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = useClaimError(claim.id);
  const [amount, setAmount] = useState('');
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (next: string) =>
      putJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}`,
        { amount: next, date: claim.date, note: claim.note, corrects: claim.corrects ?? '' },
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The amount could not be saved.'),
  });

  return (
    <td>
      <input
        data-testid="claim-inline-amount"
        placeholder="amount"
        value={amount}
        onChange={(e) => {
          setAmount(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-inline-save"
        disabled={save.isPending}
        onClick={() => {
          setError('');
          once(() => save.mutateAsync(amount.trim()));
        }}
      >
        Save amount
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 95, Component: ClaimInlineAmount });
