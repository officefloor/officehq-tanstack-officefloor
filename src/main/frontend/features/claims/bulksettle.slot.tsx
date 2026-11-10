import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { claimsKey, useLastBatch } from './claims';
import { clearSelectedClaims, useSelectedClaims } from './selection';

type Result = { settled: number; refused: number };

// Settles the ticked claims together, as the signed-in user. The server settles each that can be
// settled in list order (people in list order, each person's claims newest first) by the rules of
// settling one — a claim sent to PENDING counts as settled — and leaves the rest as they are; the
// bar shows how many were settled and how many refused. The counts are cleared while it runs.
// A bulk settle settling at least one claim is a batch, numbered on the sheet; the bar shows the
// last batch's number ('' when there has been none).
function BulkSettle({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedClaims(sheetId);
  const [user] = useUser();
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState('');
  const lastBatch = useLastBatch(sheetId).data ?? '';

  const once = useOnce();
  const settle = useMutation({
    mutationFn: (claimIds: string[]) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/bulk-settle`, { claimIds }, user),
    onSuccess: (r) => {
      setResult(r);
      clearSelectedClaims(sheetId);
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: () => setError('The selected claims could not be settled.'),
  });

  const press = () => {
    setResult(null);
    setError('');
    const ids = [...selected];
    once((done) => settle.mutate(ids, { onSettled: done }));
  };

  return (
    <section data-testid="bulk-settle-claims">
      <button type="button" data-testid="bulk-settle" onClick={press} disabled={settle.isPending}>
        Settle selected ({selected.size})
      </button>{' '}
      Last batch <span data-testid="bulk-settle-batch">{lastBatch}</span>
      {result !== null && (
        <>
          {' '}Settled <span data-testid="bulk-settled">{result.settled}</span>, refused{' '}
          <span data-testid="bulk-settle-refused">{result.refused}</span>
        </>
      )}
      {error && (
        <p role="alert" data-testid="bulk-settle-error">
          {error}
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6, Component: BulkSettle });
