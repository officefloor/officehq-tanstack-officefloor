import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { useSelectedClaims } from './useSelectedClaims';

/**
 * Open the ticked claims together. Each that can be opened is opened and numbered in list order
 * (people in list order, each person's newest claim first); each that cannot is left as it is.
 * Shows how many were opened and how many refused; the counts go while the next press runs.
 */
function BulkOpen({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useSelectedClaims();
  const bulk = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<{ opened: number; refused: number }>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-open`, { claimIds, user }),
    onSuccess: () => setSelected([]),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });
  return (
    <section data-testid="bulk-open-panel">
      <button
        type="button"
        data-testid="bulk-open"
        disabled={bulk.isPending}
        onClick={() => {
          bulk.reset();
          bulk.mutate(selected);
        }}
      >
        Open selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <span>
          {' '}
          Opened <strong data-testid="bulk-opened">{bulk.data.opened}</strong>, refused{' '}
          <strong data-testid="bulk-refused">{bulk.data.refused}</strong>
        </span>
      )}
      {bulk.isError && (
        <span data-testid="bulk-open-error" role="alert">
          {' '}
          {selected.length === 0 ? 'Select the claims to open.' : 'Could not open the selected claims.'}
        </span>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 15, Component: BulkOpen });
