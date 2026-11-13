import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { claimsKey, useClaims } from './claims';
import type { Claim } from './claims';
import { useSelectedClaims } from './selection';
import { useOneAtATime } from '../../query/useOneAtATime';

// Settles every ticked claim, one after another in list order (people in their order, each one's
// claims newest first), as the signed-in user. Each goes through the claim's own settle action, so a
// clerk's settle above the clerk limit makes it PENDING and still counts as settled; one the server
// refuses is left as it is and counted as refused.
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

  if (!user || !sheet) return null;

  const run = async () => {
    setResult(undefined);
    setBusy(true);
    const ordered = people.flatMap((p) => claims.filter((c) => c.personId === p.id && selected.has(c.id)));
    let settled = 0;
    let refused = 0;
    try {
      for (const c of ordered) {
        try {
          await postJson<Claim>(
            `/api/sheets/${encodeURIComponent(sheet)}/claims/${c.id}/settle?user=${encodeURIComponent(user)}`,
            {},
          );
          settled++;
        } catch {
          refused++;
        }
      }
      // A ticked claim no longer listed (gone from the sheet) cannot be settled.
      refused += selected.size - ordered.length;
      clear();
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    } finally {
      setBusy(false);
      setResult({ settled, refused });
    }
  };

  return (
    <p>
      <button type="button" data-testid="bulk-settle" disabled={busy} onClick={() => once(run)}>
        Settle selected claims
      </button>
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
