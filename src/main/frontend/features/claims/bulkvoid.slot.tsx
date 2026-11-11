import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { peopleKey } from '../people/people';
import { useUser } from '../session/session';
import { useVoidReasons } from '../voidreasons/voidReasons';
import { claimsKey } from './claims';
import { clearSelectedClaims, useSelectedClaims } from './selection';

type Result = { voided: number; refused: number };

// Voids the ticked claims together, as the signed-in user, with the reason chosen (offered while the
// sheet has void reasons). The server voids each that can be voided in list order (people in list
// order, each person's claims newest first) by the rules of voiding one, and leaves the rest as they
// are; the bar shows how many were voided and how many refused. The counts are cleared while it runs.
function BulkVoid({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedClaims(sheetId);
  const [user] = useUser();
  const reasons = (useVoidReasons(sheetId).data ?? []).map((r) => r.reason);
  const [reason, setReason] = useState('');
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState('');

  const once = useOnce();
  const voidClaims = useMutation({
    mutationFn: ({ claimIds, reason }: { claimIds: string[]; reason: string }) =>
      postJson<Result>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/bulk-void`,
        { claimIds, reason },
        user,
      ),
    onSuccess: (r) => {
      setResult(r);
      clearSelectedClaims(sheetId);
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: () => setError('The selected claims could not be voided.'),
  });

  const press = () => {
    setResult(null);
    setError('');
    const claimIds = [...selected];
    const r = reasons.includes(reason) ? reason : '';
    once((done) => voidClaims.mutate({ claimIds, reason: r }, { onSettled: done }));
  };

  return (
    <section data-testid="bulk-void-claims">
      {reasons.length > 0 && (
        <>
          <select
            data-testid="bulk-void-reason"
            aria-label="Void reason for selected"
            value={reason}
            onChange={(e) => {
              setReason(e.target.value);
              setError('');
            }}
          >
            <option value="">No void reason</option>
            {reasons.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>{' '}
        </>
      )}
      <button type="button" data-testid="bulk-void" onClick={press} disabled={voidClaims.isPending}>
        Void selected ({selected.size})
      </button>
      {result !== null && (
        <>
          {' '}Voided <span data-testid="bulk-voided">{result.voided}</span>, refused{' '}
          <span data-testid="bulk-void-refused">{result.refused}</span>
        </>
      )}
      {error && (
        <p role="alert" data-testid="bulk-void-error">
          {error}
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6.7, Component: BulkVoid });
