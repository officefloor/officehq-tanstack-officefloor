import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { claimsKey, useClaims } from './claims';
import type { Claim } from './claims';
import { useSelectedClaims } from './selection';
import { useOneAtATime } from '../../query/useOneAtATime';

// Re-dates every ticked claim to the date typed beside the button, one after another in list order
// (people in their order, each one's claims newest first), as the signed-in user. Each goes through
// the claim's own re-date, so it is re-dated by exactly its rules (steward only, OPEN or PENDING,
// retirement, closed period) and audited the same; one the server refuses is left as it is and
// counted as refused.
export function BulkRedate() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const [selected, , clear] = useSelectedClaims();
  const [date, setDate] = useState('');
  const [result, setResult] = useState<{ redated: number; refused: number } | undefined>(undefined);
  const [busy, setBusy] = useState(false);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  if (!user || !sheet) return null;

  const run = async () => {
    setResult(undefined);
    setBusy(true);
    const ordered = people.flatMap((p) => claims.filter((c) => c.personId === p.id && selected.has(c.id)));
    const to = date.trim();
    let redated = 0;
    let refused = 0;
    try {
      for (const c of ordered) {
        try {
          if (!/^\d{4}-\d{2}-\d{2}$/.test(to)) throw new Error('Type the new date as yyyy-mm-dd.');
          await putJson<Claim>(
            `/api/sheets/${encodeURIComponent(sheet)}/claims/${c.id}/date?user=${encodeURIComponent(user)}`,
            { date: to },
          );
          redated++;
        } catch {
          refused++;
        }
      }
      // A ticked claim no longer listed (gone from the sheet) cannot be re-dated.
      refused += selected.size - ordered.length;
      clear();
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    } finally {
      setBusy(false);
      setResult({ redated, refused });
    }
  };

  return (
    <p>
      <input
        data-testid="bulk-redate-date"
        aria-label="Re-date selected claims to"
        placeholder="re-date to yyyy-mm-dd"
        value={date}
        onChange={(e) => setDate(e.target.value)}
      />{' '}
      <button type="button" data-testid="bulk-redate" disabled={busy} onClick={() => once(run)}>
        Re-date selected claims
      </button>
      {result && (
        <>
          {' '}
          Re-dated <span data-testid="bulk-redated">{result.redated}</span>, refused{' '}
          <span data-testid="bulk-redate-refused">{result.refused}</span>
        </>
      )}
    </p>
  );
}
