import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { claimsKey, useClaims, useLastBatch } from './claims';
import { clearSelected, useSelectedClaims } from './selection';

type BulkSettleResult = { settled: number; refused: number };

/** The button that settles every selected claim together, by the signed-in user, all or nothing. When any selected
 *  claim cannot be settled by the rules of settling one, the server settles none and counts every one refused;
 *  otherwise it settles each in list order (a clerk's settle that leaves it PENDING counts as settled). The bar then
 *  shows how many were settled and how many refused. One that settles is the sheet's next batch; beside the button is
 *  the number of its last (nothing when none). */
function BulkSettle({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const claims = useClaims(sheetId).data ?? [];
  const chosen = useSelectedClaims(sheetId);
  const lastBatch = useLastBatch(sheetId).data?.last ?? null;
  const ids = claims.filter((c) => chosen.has(c.id)).map((c) => c.id);
  const [result, setResult] = useState<BulkSettleResult | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const settle = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<BulkSettleResult>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-settle`, { user, claimIds }),
    onSuccess: async (r) => {
      clearSelected(sheetId);
      setResult(r);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not settle the selected claims.'),
    onSettled: once.done,
  });

  return (
    <p>
      <button
        type="button"
        data-testid="bulk-settle"
        disabled={settle.isPending}
        onClick={() => {
          setError('');
          setResult(null);
          if (ids.length === 0) {
            setError('Select the claims to settle.');
            return;
          }
          if (!once.start()) return;
          settle.mutate(ids);
        }}
      >
        Settle selected
      </button>
      {' '}Last batch <span data-testid="bulk-settle-batch">{lastBatch ?? ''}</span>
      {result && (
        <>
          {' '}Settled <span data-testid="bulk-settled">{result.settled}</span>, refused{' '}
          <span data-testid="bulk-settle-refused">{result.refused}</span>
        </>
      )}
      {error && (
        <span data-testid="bulk-settle-error" role="alert">
          {' '}{error}
        </span>
      )}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 13, Component: BulkSettle });
