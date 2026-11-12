import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { claimsKey, useClaims, useSelectedClaims } from './claims';

type BulkSettleResult = { settled: number; refused: number };

// Settles the selected claims together: the server settles each in list order by the rules of a
// single settle (a clerk's above the clerk limit goes to PENDING, counted as settled) and refuses
// the rest, and the counts show once it answers (gone while it runs). One that settles any is a batch,
// numbered on the sheet; the last batch's number shows beside the button (nothing when there is none).
function BulkSettle() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [selected, setSelected] = useSelectedClaims();
  const queryClient = useQueryClient();
  const claims = useClaims(sheet);
  const lastBatch = (claims.data ?? []).reduce<number | null>(
    (last, c) => (c.batch !== null && (last === null || c.batch > last) ? c.batch : last), null);

  const bulk = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<BulkSettleResult>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-settle`, {
        claimIds: claimIds.map(String),
        user,
      }),
    onSuccess: async () => {
      setSelected([]);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });
  const submit = useSubmitOnce(bulk);

  return (
    <p>
      <button type="button" data-testid="bulk-settle" disabled={bulk.isPending} onClick={() => submit(selected)}>
        Settle selected
      </button>{' '}
      <span data-testid="bulk-settle-batch">{lastBatch ?? ''}</span>
      {bulk.isSuccess && (
        <>
          {' '}
          Settled <strong data-testid="bulk-settled">{bulk.data.settled}</strong>, refused{' '}
          <strong data-testid="bulk-settle-refused">{bulk.data.refused}</strong>
        </>
      )}
      {bulk.isError && (
        <span data-testid="bulk-settle-error" role="alert">
          {' '}
          {bulk.error instanceof HttpError && bulk.error.reason ? `${bulk.error.reason}.` : 'The bulk settle was refused.'}
        </span>
      )}
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 23, Component: BulkSettle });
