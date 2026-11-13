import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { claimsKey, settleBatchesKey, useClaims, useLastBatch } from './claims';
import type { Claim } from './claims';
import { useSelectedClaims } from './selection';
import { useOneAtATime } from '../../query/useOneAtATime';

// Settles every ticked claim, one after another in list order (people in their order, each one's
// claims newest first), as the signed-in user — all or nothing: the server first checks every one, and
// if any would be refused none is settled and every one counts as refused. Each goes through the claim's
// own settle action, so a clerk's settle above the clerk limit makes it PENDING and still counts as
// settled; afterwards the bulk settle's BATCH_SETTLED line is written. Every settle carries one token for the whole bulk settle, so the
// claims it settles share one batch number (the sheet's next, taken by the first); beside the button is the sheet's last.
export function BulkSettle() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const [selected, , clear] = useSelectedClaims();
  const [result, setResult] = useState<{ settled: number; refused: number } | undefined>(undefined);
  const [busy, setBusy] = useState(false);
  const queryClient = useQueryClient();
  const once = useOneAtATime();
  const lastBatch = useLastBatch().data?.last ?? null;

  if (!user || !sheet) return null;

  const run = async () => {
    setResult(undefined);
    setBusy(true);
    const ordered = people.flatMap((p) => claims.filter((c) => c.personId === p.id && selected.has(c.id)));
    let settled = 0;
    let refused = 0;
    const batch = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
    const base = `/api/sheets/${encodeURIComponent(sheet)}`;
    const asUser = `?user=${encodeURIComponent(user)}`;
    try {
      // All or nothing: a ticked claim no longer listed (gone from the sheet), or one the server would refuse to
      // settle, refuses every one and nothing is settled.
      let allowed = ordered.length === selected.size;
      if (allowed && ordered.length > 0) {
        try {
          await postJson(`${base}/settle-batches/check${asUser}`, { claimIds: ordered.map((c) => c.id), batch });
        } catch {
          allowed = false;
        }
      }
      if (!allowed) {
        refused = selected.size;
      } else {
        for (const c of ordered) {
          try {
            await postJson<Claim>(`${base}/claims/${c.id}/settle${asUser}`, { batch });
            settled++;
          } catch {
            refused++;
          }
        }
        if (settled > 0) await postJson(`${base}/settle-batches/done${asUser}`, { batch });
      }
      clear();
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: settleBatchesKey(sheet) }),
      ]);
    } finally {
      setBusy(false);
      setResult({ settled, refused });
    }
  };

  return (
    <p>
      <button type="button" data-testid="bulk-settle" disabled={busy} onClick={() => once(run)}>
        Settle selected claims
      </button>{' '}
      <span data-testid="bulk-settle-batch">{lastBatch ?? ''}</span>
      {result && (
        <>
          {' '}
          Settled <span data-testid="bulk-settled">{result.settled}</span>, refused{' '}
          <span data-testid="bulk-settle-refused">{result.refused}</span>
        </>
      )}
    </p>
  );
}
