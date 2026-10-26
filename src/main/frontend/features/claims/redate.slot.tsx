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
 * The entry and button with which a steward re-dates an OPEN or PENDING claim. The server refuses anyone else, any
 * other status, a date after the person's retirement, and a claim due by a date in a closed period (its due-by, and so its period, stay); the
 * refusal shows on the claim until its next action.
 */
function ClaimRedate({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useClaimError(claim.id);
  const [date, setDate] = useState('');
  const once = useSubmitOnce();
  const redate = useMutation({
    mutationFn: (next: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/date`, { date: next }, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The claim could not be re-dated.'),
  });

  return (
    <td>
      <input
        data-testid="claim-date-new"
        placeholder="yyyy-mm-dd"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-redate"
        disabled={redate.isPending}
        onClick={() => {
          setError('');
          once(() => redate.mutateAsync(date.trim()));
        }}
      >
        Re-date
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 26, Component: ClaimRedate });
