import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { bulkOpenClaims, claimsKey } from './api';

/** A button that opens the selected claims together, and how many it opened and refused. The counts
 *  are cleared while it runs; the selection is cleared once it is done. */
export function BulkOpen({ sheetId, user, selected, onDone }: {
  sheetId: string; user: string; selected: number[]; onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const runOnce = useSubmitOnce();
  const bulk = useMutation({
    mutationFn: (ids: number[]) => bulkOpenClaims(sheetId, user, ids),
    onSuccess: () => {
      onDone();
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <div>
      <button type="button" data-testid="bulk-open" disabled={bulk.isPending}
        onClick={() => {
          const ids = [...selected];
          runOnce(() => {
            bulk.reset();
            return bulk.mutateAsync(ids);
          });
        }}>
        Open selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}Opened <span data-testid="bulk-opened">{bulk.data.opened}</span>,{' '}
          refused <span data-testid="bulk-refused">{bulk.data.refused}</span>
        </>
      )}
      {bulk.isError && <span data-testid="bulk-open-error">The selected claims could not be opened.</span>}
    </div>
  );
}
