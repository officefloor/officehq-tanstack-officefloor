import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { claimsKey } from '../claims/claims';
import { useSelectedClaims } from '../claims/selection';

type Result = { settled: number; refused: number };

const batchKey = (sheetId: string) => ['sheet', sheetId, 'settle-batch'];

/** Settles the selected claims together by the signed-in user: each is settled in list order (people in order, a
 *  person's claims newest first) by the rules of the single settle, a clerk's settle above the limit making it PENDING
 *  (counted settled); each that cannot is left as it is. Shows how many were settled and how many refused; the counts
 *  are cleared while a new press runs. Beside the button: the number of the sheet's last batch (a bulk settle that
 *  settled at least one claim), or nothing when there has been none. */
function BulkSettle({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useSelectedClaims();
  const batch = useQuery({
    queryKey: batchKey(sheetId),
    queryFn: () => getJson<{ last: number | null }>(`/api/sheets/${encodeURIComponent(sheetId)}/settle-batch`),
  });
  const settle = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-settle`, { claimIds, by: user }),
    onSuccess: async () => {
      setSelected([]);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: batchKey(sheetId) }),
      ]);
    },
  });

  return (
    <p>
      <button
        type="button"
        data-testid="bulk-settle"
        disabled={settle.isPending}
        onClick={() => {
          settle.reset();
          settle.mutate(selected);
        }}
      >
        Settle selected ({selected.length})
      </button>{' '}
      Last batch <span data-testid="bulk-settle-batch">{batch.data?.last ?? ''}</span>
      {settle.isSuccess && (
        <>
          {' '}
          Settled <span data-testid="bulk-settled">{settle.data.settled}</span>, refused{' '}
          <span data-testid="bulk-settle-refused">{settle.data.refused}</span>
        </>
      )}
      {settle.isError && <span data-testid="bulk-settle-error"> Could not settle the selected claims.</span>}
    </p>
  );
}

export const contribution = SheetPage.fill({ order: 26, Component: BulkSettle });
