import { useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { sheetQueryKey, useCurrentSheet, useCurrentUser } from '../session/session';
import { usePeople, useShowRetired } from '../people/people';
import { useVoidReasons } from '../voidreasons/voidreasons';
import { claimsPath, claimsQueryKey, useClaims, useSelectedClaims, type Claim } from './claims';

// Voids every selected claim with one button, in list order: the people as listed, newest claim
// first within a person. Each is voided as its own Void button would, with the reason picked beside
// the button (offered while the sheet has void reasons; none picked voids without one), so the server
// refuses what it refuses there and that claim is left as it is; the counts of voided and refused
// show once all are done, and are gone while it runs.
function BulkVoid() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const voidReasons = useVoidReasons();
  const [showRetired] = useShowRetired();
  const { selected, clear } = useSelectedClaims();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [result, setResult] = useState<{ voided: number; refused: number } | null>(null);
  const busy = useRef(false);
  if (!user || !sheet) return null;

  const reasons = voidReasons.data ?? [];
  const run = async () => {
    if (busy.current) return;
    busy.current = true;
    setResult(null);
    try {
      const listed = (people.data ?? []).filter((p) => showRetired || p.retiredFrom === null);
      const order = listed.flatMap((p) => (claims.data ?? []).filter((c) => c.personId === p.id && selected.has(c.id)));
      const query = reason !== '' && reasons.some((r) => r.reason === reason) ? `?reason=${encodeURIComponent(reason)}` : '';
      let voided = 0;
      let refused = 0;
      for (const c of order) {
        try {
          await putJson<Claim>(`${claimsPath(sheet)}/${c.id}/void${query}`, undefined, user);
          voided++;
        } catch {
          refused++;
        }
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: sheetQueryKey(sheet) }),
      ]);
      clear();
      setResult({ voided, refused });
    } finally {
      busy.current = false;
    }
  };

  return (
    <div>
      {reasons.length > 0 && (
        <>
          <select data-testid="bulk-void-reason" value={reason} onChange={(e) => setReason(e.target.value)}>
            <option value="">(no void reason)</option>
            {reasons.map((r) => (
              <option key={r.id} value={r.reason}>{r.reason}</option>
            ))}
          </select>{' '}
        </>
      )}
      <button type="button" data-testid="bulk-void" onClick={() => void run()}>
        Void selected claims
      </button>
      {result && (
        <>
          {' '}Voided <span data-testid="bulk-voided">{result.voided}</span>, refused{' '}
          <span data-testid="bulk-void-refused">{result.refused}</span>
        </>
      )}
    </div>
  );
}

export const contribution = HomeMain.fill({ order: 23.7, Component: BulkVoid });
