import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleQuery } from '../people/peopleQuery';
import { claimsKey, claimsQuery, settleBatchQuery } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useSelectedClaims } from './useSelectedClaims';

type Result = { settled: number; refused: number };

/**
 * Settles every ticked claim as the signed-in user, one at a time in list order (people in their list
 * order, newest claim first within a person), by the same rules as settling one claim. A claim that
 * goes to PENDING counts as settled; a claim the server refuses to settle is left as it is and counted
 * as refused. A bulk settle that settled at least one claim is recorded as the sheet's next settle batch, whose number
 * its claims show; the number of the last batch shows beside the button (blank when there has been none).
 */
function BulkSettle({ user, sheetId }: { user: string; sheetId: string }) {
  const queryClient = useQueryClient();
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const lastBatch = useQuery(settleBatchQuery(sheetId)).data?.last ?? null;
  const [selected, setSelected] = useSelectedClaims();
  const [result, setResult] = useState<Result | undefined>();
  const once = useSubmitOnce();

  const ordered = people.flatMap((p) => claims.filter((c) => c.personId === p.id && selected.includes(c.id)));

  const settle = useMutation({
    mutationFn: async (targets: Claim[]): Promise<Result> => {
      const settled: number[] = [];
      for (const c of targets) {
        try {
          await putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/status/SETTLED`, {}, user);
          settled.push(c.id);
        } catch {
          // refused: left as it is
        }
      }
      if (settled.length > 0) {
        await postJson(`/api/sheets/${encodeURIComponent(sheetId)}/settle-batches`, { claimIds: settled }, user);
      }
      return { settled: settled.length, refused: targets.length - settled.length };
    },
    onSuccess: async (r) => {
      setSelected([]);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      setResult(r);
    },
  });

  return (
    <section data-testid="bulk-settle-panel">
      <button
        type="button"
        data-testid="bulk-settle"
        disabled={settle.isPending}
        onClick={() => {
          setResult(undefined);
          once(() => settle.mutateAsync(ordered));
        }}
      >
        Settle selected
      </button>{' '}
      Last batch <span data-testid="bulk-settle-batch">{lastBatch ?? ''}</span>
      {result && (
        <>
          {' '}
          Settled <span data-testid="bulk-settled">{result.settled}</span>, refused{' '}
          <span data-testid="bulk-settle-refused">{result.refused}</span>
        </>
      )}
    </section>
  );
}

export const contribution = SheetPanel.fill({
  order: 22,
  Component: ({ user, sheetId }) => <BulkSettle key={sheetId} user={user} sheetId={sheetId} />,
});
