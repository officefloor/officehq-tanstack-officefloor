import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { useVoidReasons, voidReasonsKey } from '../voidreasons/voidreasons';
import { claimsKey, useClaims } from './claims';
import { clearSelected, useSelectedClaims } from './selection';

type BulkVoidResult = { voided: number; refused: number };

/** The void reason (while the sheet has any) and button that void every selected claim together, by the signed-in
 *  user. The server voids each that can be voided, in list order and by the rules of voiding one, and leaves the rest
 *  as they are; the bar then shows how many were voided and how many refused. */
function BulkVoid({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const claims = useClaims(sheetId).data ?? [];
  const voidReasons = useVoidReasons(sheetId).data ?? [];
  const chosen = useSelectedClaims(sheetId);
  const ids = claims.filter((c) => chosen.has(c.id)).map((c) => c.id);
  const [reason, setReason] = useState('');
  const [result, setResult] = useState<BulkVoidResult | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const voidAll = useMutation({
    mutationFn: (body: { claimIds: number[]; reason?: string }) =>
      postJson<BulkVoidResult>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-void`, { user, ...body }),
    onSuccess: async (r) => {
      clearSelected(sheetId);
      setReason('');
      setResult(r);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      await queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not void the selected claims.'),
    onSettled: once.done,
  });

  return (
    <p>
      {voidReasons.length > 0 && (
        <>
          <select
            data-testid="bulk-void-reason"
            value={reason}
            onChange={(e) => {
              setError('');
              setReason(e.target.value);
            }}
          >
            <option value="">Void reason…</option>
            {voidReasons.map((r) => (
              <option key={r.id} value={r.reason}>
                {r.reason}
              </option>
            ))}
          </select>{' '}
        </>
      )}
      <button
        type="button"
        data-testid="bulk-void"
        disabled={voidAll.isPending}
        onClick={() => {
          setError('');
          setResult(null);
          if (ids.length === 0) {
            setError('Select the claims to void.');
            return;
          }
          if (!once.start()) return;
          voidAll.mutate(reason !== '' ? { claimIds: ids, reason } : { claimIds: ids });
        }}
      >
        Void selected
      </button>
      {result && (
        <>
          {' '}Voided <span data-testid="bulk-voided">{result.voided}</span>, refused{' '}
          <span data-testid="bulk-void-refused">{result.refused}</span>
        </>
      )}
      {error && (
        <span data-testid="bulk-void-error" role="alert">
          {' '}{error}
        </span>
      )}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 13.7, Component: BulkVoid });
