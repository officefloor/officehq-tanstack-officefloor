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
 * The entry and button with which a steward sets the due-by of a DRAFT, OPEN or PENDING claim directly. It stays as
 * set (a holiday added later does not move it). The server refuses anyone else, any other status, a date before the
 * claim's date, and a claim or a due-by in a closed period; the refusal shows on the claim until its next action.
 */
function ClaimDueBy({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useClaimError(claim.id);
  const [date, setDate] = useState('');
  const once = useSubmitOnce();
  const setDueBy = useMutation({
    mutationFn: (next: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/due-by`, { dueBy: next }, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The due-by could not be set.'),
  });

  return (
    <td>
      <input
        data-testid="claim-due-by-new"
        placeholder="yyyy-mm-dd"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          if (error) setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-set-due-by"
        disabled={setDueBy.isPending}
        onClick={() => {
          setError('');
          once(() => setDueBy.mutateAsync(date.trim()));
        }}
      >
        Set due-by
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 26.5, Component: ClaimDueBy });
