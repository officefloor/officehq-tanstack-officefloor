import { useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { sheetQueryKey, useCurrentSheet, useCurrentUser, useSheetInfo } from '../session/session';
import { usePeople, useShowRetired } from '../people/people';
import { claimsPath, claimsQueryKey, useClaims, useSelectedClaims, type Claim } from './claims';

// Settles every selected claim with one button, in list order: the people as listed, newest claim
// first within a person. Each is settled as its own Settle button would, by the signed-in user (the
// server refuses what it refuses there, and that claim is left as it is; one a clerk settles above
// the clerk limit goes PENDING, and counts as settled); the counts of settled and refused show once
// all are done, and are gone while it runs. Every settle of one press carries the same batch key: a press that
// settles at least one claim is a batch, numbered by the server; the sheet's last batch number shows beside the button.
function BulkSettle() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const [showRetired] = useShowRetired();
  const { selected, clear } = useSelectedClaims();
  const info = useSheetInfo();
  const queryClient = useQueryClient();
  const [result, setResult] = useState<{ settled: number; refused: number } | null>(null);
  const busy = useRef(false);
  if (!user || !sheet) return null;

  const run = async () => {
    if (busy.current) return;
    busy.current = true;
    setResult(null);
    try {
      const listed = (people.data ?? []).filter((p) => showRetired || p.retiredFrom === null);
      const order = listed.flatMap((p) => (claims.data ?? []).filter((c) => c.personId === p.id && selected.has(c.id)));
      let settled = 0;
      let refused = 0;
      const batch = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
      for (const c of order) {
        try {
          await putJson<Claim>(`${claimsPath(sheet)}/${c.id}/settle?batch=${encodeURIComponent(batch)}`, undefined, user);
          settled++;
        } catch {
          refused++;
        }
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: sheetQueryKey(sheet) }),
      ]);
      clear();
      setResult({ settled, refused });
    } finally {
      busy.current = false;
    }
  };

  return (
    <div>
      <button type="button" data-testid="bulk-settle" onClick={() => void run()}>
        Settle selected claims
      </button>{' '}
      Last batch <span data-testid="bulk-settle-batch">{info.data?.lastBatch ?? ''}</span>
      {result && (
        <>
          {' '}Settled <span data-testid="bulk-settled">{result.settled}</span>, refused{' '}
          <span data-testid="bulk-settle-refused">{result.refused}</span>
        </>
      )}
    </div>
  );
}

export const contribution = HomeMain.fill({ order: 22, Component: BulkSettle });
