import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { bulkSettleClaims, claimsKey, lastSettleBatch, settleBatchKey } from './api';

/** A button that settles the selected claims together, and how many it settled and refused. The
 *  counts are cleared while it runs; the selection is cleared once it is done. Beside it, the number of
 *  the sheet's last batch (blank: none yet). */
export function BulkSettle({ sheetId, user, selected, onDone }: {
  sheetId: string; user: string; selected: number[]; onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const runOnce = useSubmitOnce();
  const batch = useQuery({ queryKey: settleBatchKey(sheetId), queryFn: () => lastSettleBatch(sheetId) });
  const bulk = useMutation({
    mutationFn: (ids: number[]) => bulkSettleClaims(sheetId, user, ids),
    onSuccess: () => {
      onDone();
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <div>
      <button type="button" data-testid="bulk-settle" disabled={bulk.isPending}
        onClick={() => {
          const ids = [...selected];
          runOnce(() => {
            bulk.reset();
            return bulk.mutateAsync(ids);
          });
        }}>
        Settle selected ({selected.length})
      </button>
      {' '}Last batch <span data-testid="bulk-settle-batch">{batch.data?.last ?? ''}</span>
      {bulk.isSuccess && (
        <>
          {' '}Settled <span data-testid="bulk-settled">{bulk.data.settled}</span>,{' '}
          refused <span data-testid="bulk-settle-refused">{bulk.data.refused}</span>
        </>
      )}
      {bulk.isError && <span data-testid="bulk-settle-error">The selected claims could not be settled.</span>}
    </div>
  );
}
