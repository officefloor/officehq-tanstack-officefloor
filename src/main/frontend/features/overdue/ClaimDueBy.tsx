import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Claim } from '../claims/claims';
import { claimsKey } from '../claims/claims';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useClaimError } from '../claims/claimError';

// A claim's due-by: fixed when it was created, from the terms of its person's grade in force on its date. A steward
// sets a DRAFT, OPEN or PENDING claim's due-by directly to the date typed beside the Set due-by button; one set so stays
// as set (a holiday added later does not move it). The server refuses anyone else or any other status, and the refusal
// shows as the claim's error.
export function ClaimDueBy({ claim }: { claim: Claim }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = useClaimError(claim.id);
  const [dueByTo, setDueByTo] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const setDueBy = useMutation({
    mutationFn: (dueBy: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/due-by?user=${encodeURIComponent(user)}`, {
        dueBy,
      }),
    onSuccess: async () => {
      setDueByTo('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () =>
      setError(
        `This ${claim.status} claim's due-by cannot be set to that date (only a steward sets the due-by of a DRAFT, OPEN or PENDING claim, outside a closed period).`,
      ),
  });

  return (
    <>
      <span data-testid="claim-due-by">{claim.dueBy}</span>{' '}
      <input
        data-testid="claim-due-by-new"
        aria-label="New due-by"
        placeholder="due by yyyy-mm-dd"
        value={dueByTo}
        onChange={(e) => {
          setDueByTo(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-set-due-by"
        disabled={setDueBy.isPending}
        onClick={() => {
          setError('');
          const dueBy = dueByTo.trim();
          if (!/^\d{4}-\d{2}-\d{2}$/.test(dueBy)) {
            setError('Type the claim\'s new due-by as yyyy-mm-dd.');
            return;
          }
          once(() => setDueBy.mutateAsync(dueBy));
        }}
      >
        Set due-by
      </button>
    </>
  );
}
