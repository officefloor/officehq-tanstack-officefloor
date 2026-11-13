import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { claimsKey, useClaims } from './claims';
import type { Claim } from './claims';
import { useSelectedClaims } from './selection';
import { useOneAtATime } from '../../query/useOneAtATime';

// Moves every ticked claim to the person chosen beside the button, one after another in list order
// (people in their order, each one's claims newest first), as the signed-in user. Each goes through
// the claim's own move, so it is moved by exactly its rules (role, status, closed period, retirement)
// and audited the same; one the server refuses is left as it is and counted as refused.
export function BulkReassign() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const [selected, , clear] = useSelectedClaims();
  const [moveTo, setMoveTo] = useState('');
  const [result, setResult] = useState<{ reassigned: number; refused: number } | undefined>(undefined);
  const [busy, setBusy] = useState(false);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  if (!user || !sheet) return null;

  const run = async () => {
    setResult(undefined);
    setBusy(true);
    const ordered = people.flatMap((p) => claims.filter((c) => c.personId === p.id && selected.has(c.id)));
    let reassigned = 0;
    let refused = 0;
    try {
      for (const c of ordered) {
        try {
          if (!moveTo) throw new Error('Choose the person to move the claims to.');
          await putJson<Claim>(
            `/api/sheets/${encodeURIComponent(sheet)}/claims/${c.id}/person?user=${encodeURIComponent(user)}`,
            { personId: Number(moveTo), bulk: true },
          );
          reassigned++;
        } catch {
          refused++;
        }
      }
      // A ticked claim no longer listed (gone from the sheet) cannot be moved.
      refused += selected.size - ordered.length;
      clear();
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    } finally {
      setBusy(false);
      setResult({ reassigned, refused });
    }
  };

  return (
    <p>
      <select
        data-testid="bulk-reassign-to"
        aria-label="Move selected claims to"
        value={moveTo}
        onChange={(e) => setMoveTo(e.target.value)}
      >
        <option value="">move to…</option>
        {people.map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button type="button" data-testid="bulk-reassign" disabled={busy} onClick={() => once(run)}>
        Move selected claims
      </button>
      {result && (
        <>
          {' '}
          Moved <span data-testid="bulk-reassigned">{result.reassigned}</span>, refused{' '}
          <span data-testid="bulk-reassign-refused">{result.refused}</span>
        </>
      )}
    </p>
  );
}
