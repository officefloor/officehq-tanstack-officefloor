import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { claimsKey, useClaims } from './claims';
import type { Claim } from './claims';
import { useSelectedClaims } from './selection';
import { useOneAtATime } from '../../query/useOneAtATime';

// Opens every ticked claim, one after another in list order (people in their order, each one's
// claims newest first), so they are numbered in that order. Each goes through the claim's own open
// action: one the server refuses is left as it is and counted as refused; one that went to REVIEW counts as opened.
export function BulkOpen() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const [selected, , clear] = useSelectedClaims();
  const [result, setResult] = useState<{ opened: number; refused: number } | undefined>(undefined);
  const [busy, setBusy] = useState(false);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  if (!user || !sheet) return null;

  const run = async () => {
    setResult(undefined);
    setBusy(true);
    const ordered = people.flatMap((p) => claims.filter((c) => c.personId === p.id && selected.has(c.id)));
    let opened = 0;
    let refused = 0;
    try {
      for (const c of ordered) {
        try {
          await postJson<Claim>(
            `/api/sheets/${encodeURIComponent(sheet)}/claims/${c.id}/open?user=${encodeURIComponent(user)}`,
            {},
          );
          opened++;
        } catch {
          refused++;
        }
      }
      // A ticked claim no longer listed (gone from the sheet) cannot be opened.
      refused += selected.size - ordered.length;
      clear();
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    } finally {
      setBusy(false);
      setResult({ opened, refused });
    }
  };

  return (
    <p>
      <button type="button" data-testid="bulk-open" disabled={busy} onClick={() => once(run)}>
        Open selected claims
      </button>
      {result && (
        <>
          {' '}
          Opened <span data-testid="bulk-opened">{result.opened}</span>, refused{' '}
          <span data-testid="bulk-refused">{result.refused}</span>
        </>
      )}
    </p>
  );
}
