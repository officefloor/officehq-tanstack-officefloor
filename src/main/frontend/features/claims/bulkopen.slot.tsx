import { useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { usePeople, useShowRetired } from '../people/people';
import { claimsPath, claimsQueryKey, useClaims, useSelectedClaims, type Claim } from './claims';

// Opens every selected claim with one button, in list order: the people as listed, newest claim
// first within a person. Each is opened as its own Open
// button would (the server refuses what it refuses there, and that claim is left as it is); the
// counts of opened and refused show once all are done, and are gone while it runs.
function BulkOpen() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const [showRetired] = useShowRetired();
  const { selected, clear } = useSelectedClaims();
  const queryClient = useQueryClient();
  const [result, setResult] = useState<{ opened: number; refused: number } | null>(null);
  const busy = useRef(false);
  if (!user || !sheet) return null;

  const run = async () => {
    if (busy.current) return;
    busy.current = true;
    setResult(null);
    try {
      const listed = (people.data ?? []).filter((p) => showRetired || p.retiredFrom === null);
      const order = listed.flatMap((p) => (claims.data ?? []).filter((c) => c.personId === p.id && selected.has(c.id)));
      let opened = 0;
      let refused = 0;
      for (const c of order) {
        try {
          await putJson<Claim>(`${claimsPath(sheet)}/${c.id}/open`, undefined, user);
          opened++;
        } catch {
          refused++;
        }
      }
      await queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
      clear();
      setResult({ opened, refused });
    } finally {
      busy.current = false;
    }
  };

  return (
    <div>
      <button type="button" data-testid="bulk-open" onClick={() => void run()}>
        Open selected claims
      </button>
      {result && (
        <>
          {' '}Opened <span data-testid="bulk-opened">{result.opened}</span>, refused{' '}
          <span data-testid="bulk-refused">{result.refused}</span>
        </>
      )}
    </div>
  );
}

export const contribution = HomeMain.fill({ order: 21, Component: BulkOpen });
