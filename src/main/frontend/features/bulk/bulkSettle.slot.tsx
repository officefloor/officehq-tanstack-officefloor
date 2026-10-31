import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { useSelectedClaims } from './useSelectedClaims';

/**
 * Settle the ticked claims together, by the signed-in user. Each that can be settled is settled
 * (or goes PENDING) in list order (people in list order, each person's newest claim first); each
 * that cannot is left as it is. Shows how many were settled and how many refused; the counts go
 * while the next press runs.
 */
function BulkSettle({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useSelectedClaims();
  const bulk = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<{ settled: number; refused: number }>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-settle`, { claimIds, user }),
    onSuccess: () => setSelected([]),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });
  return (
    <section data-testid="bulk-settle-panel">
      <button
        type="button"
        data-testid="bulk-settle"
        disabled={bulk.isPending}
        onClick={() => {
          bulk.reset();
          bulk.mutate(selected);
        }}
      >
        Settle selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <span>
          {' '}
          Settled <strong data-testid="bulk-settled">{bulk.data.settled}</strong>, refused{' '}
          <strong data-testid="bulk-settle-refused">{bulk.data.refused}</strong>
        </span>
      )}
      {bulk.isError && (
        <span data-testid="bulk-settle-error" role="alert">
          {' '}
          {selected.length === 0 ? 'Select the claims to settle.' : 'Could not settle the selected claims.'}
        </span>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 16, Component: BulkSettle });
