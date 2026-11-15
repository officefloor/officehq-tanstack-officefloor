import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { bulkVoidClaims, claimsKey, listVoidReasons, voidReasonsKey } from './api';

/** A void reason select (while the sheet has void reasons) and a button that voids the selected claims
 *  together, and how many it voided and refused. The counts are cleared while it runs; the selection is
 *  cleared once it is done. */
export function BulkVoid({ sheetId, user, selected, onDone }: {
  sheetId: string; user: string; selected: number[]; onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const runOnce = useSubmitOnce();
  const [reason, setReason] = useState('');
  const reasons = useQuery({ queryKey: voidReasonsKey(sheetId), queryFn: () => listVoidReasons(sheetId) });
  const bulk = useMutation({
    mutationFn: (v: { ids: number[]; reason: string }) => bulkVoidClaims(sheetId, user, v.ids, v.reason),
    onSuccess: () => {
      onDone();
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <div>
      {(reasons.data ?? []).length > 0 && (
        <>
          <select data-testid="bulk-void-reason" value={reason}
            onChange={(e) => {
              setReason(e.target.value);
              bulk.reset();
            }}>
            <option value="">No void reason</option>
            {(reasons.data ?? []).map((r) => <option key={r.reason} value={r.reason}>{r.reason}</option>)}
          </select>{' '}
        </>
      )}
      <button type="button" data-testid="bulk-void" disabled={bulk.isPending}
        onClick={() => {
          const ids = [...selected];
          const chosen = reason;
          runOnce(() => {
            bulk.reset();
            return bulk.mutateAsync({ ids, reason: chosen });
          });
        }}>
        Void selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}Voided <span data-testid="bulk-voided">{bulk.data.voided}</span>,{' '}
          refused <span data-testid="bulk-void-refused">{bulk.data.refused}</span>
        </>
      )}
      {bulk.isError && <span data-testid="bulk-void-error">The selected claims could not be voided.</span>}
    </div>
  );
}
