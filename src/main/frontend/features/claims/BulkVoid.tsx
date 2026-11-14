import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { claimsKey, useClaims } from './claims';
import type { Claim } from './claims';
import { useSelectedClaims } from './selection';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useVoidReasons } from '../voidreasons/voidReasons';

// Voids every ticked claim, one after another in list order (people in their order, each one's
// claims newest first), as the signed-in user. Each goes through the claim's own void, so it is
// voided by exactly its rules (role, a settled claim only by a steward within 7 days, the reason
// while the sheet has void reasons) and audited the same; one the server refuses is left as it is
// and counted as refused. While the sheet has void reasons, each void sends the reason chosen
// beside the button (none chosen: the server refuses it).
export function BulkVoid() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const voidReasons = useVoidReasons().data ?? [];
  const [selected, , clear] = useSelectedClaims();
  const [reason, setReason] = useState('');
  const [result, setResult] = useState<{ voided: number; refused: number } | undefined>(undefined);
  const [busy, setBusy] = useState(false);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  if (!user || !sheet) return null;

  const run = async () => {
    setResult(undefined);
    setBusy(true);
    const ordered = people.flatMap((p) => claims.filter((c) => c.personId === p.id && selected.has(c.id)));
    let voided = 0;
    let refused = 0;
    try {
      for (const c of ordered) {
        try {
          await postJson<Claim>(
            `/api/sheets/${encodeURIComponent(sheet)}/claims/${c.id}/void?user=${encodeURIComponent(user)}`,
            { reason },
          );
          voided++;
        } catch {
          refused++;
        }
      }
      // A ticked claim no longer listed (gone from the sheet) cannot be voided.
      refused += selected.size - ordered.length;
      clear();
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    } finally {
      setBusy(false);
      setResult({ voided, refused });
    }
  };

  return (
    <p>
      {voidReasons.length > 0 && (
        <>
          <select
            data-testid="bulk-void-reason"
            aria-label="Void reason for selected claims"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          >
            <option value="">void reason…</option>
            {voidReasons.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>{' '}
        </>
      )}
      <button type="button" data-testid="bulk-void" disabled={busy} onClick={() => once(run)}>
        Void selected claims
      </button>
      {result && (
        <>
          {' '}
          Voided <span data-testid="bulk-voided">{result.voided}</span>, refused{' '}
          <span data-testid="bulk-void-refused">{result.refused}</span>
        </>
      )}
    </p>
  );
}
