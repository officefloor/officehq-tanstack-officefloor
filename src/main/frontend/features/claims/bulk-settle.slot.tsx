import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { sheetKey, useSheet } from '../sheet/sheet';
import type { Sheet } from '../sheet/sheet';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey, claimsPath } from './claims';
import type { Claim } from './claims';
import { clearSelectedClaims, useSelectedClaims } from './selection';

type BulkSettled = { settled: number; refused: number; claims: Claim[]; lastBatch: number | null };

// Settles the selected claims together as the signed-in user: the server settles each in list order (people in the
// list positions, newest claim first) by the rules of settling one, counting one sent to PENDING as settled,
// all or nothing: when any one cannot be settled none is and all count refused. Shows how many of each, and the number of the sheet's last batch (a bulk settle
// that settled at least one claim).
function BulkSettle({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedClaims(sheet);
  const lastBatch = useSheet(sheet).data?.lastBatch ?? null;
  const settle = useMutation({
    mutationFn: (ids: number[]) =>
      postJson<BulkSettled>(byUser(`${claimsPath(sheet)}/bulk-settle`, user), { claims: ids }),
    onSuccess: (result) => {
      queryClient.setQueryData(claimsKey(sheet), result.claims);
      queryClient.setQueryData<Sheet>(sheetKey(sheet), (old) => (old ? { ...old, lastBatch: result.lastBatch } : old));
      clearSelectedClaims(sheet);
    },
  });
  const act = useSubmitOnce(settle);

  return (
    <p>
      <button type="button" data-testid="bulk-settle" disabled={settle.isPending} onClick={() => act([...selected])}>
        Settle selected
      </button>{' '}
      {lastBatch !== null && 'last batch '}
      <span data-testid="bulk-settle-batch">{lastBatch ?? ''}</span>
      {settle.isSuccess && (
        <>
          {' '}Settled <strong data-testid="bulk-settled">{settle.data.settled}</strong>, refused{' '}
          <strong data-testid="bulk-settle-refused">{settle.data.refused}</strong>
        </>
      )}
      {settle.isError && (
        <span data-testid="bulk-settle-error" role="alert">
          {' '}The bulk settle failed.
        </span>
      )}
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 22, Component: BulkSettle });
