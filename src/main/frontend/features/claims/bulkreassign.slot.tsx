import { useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peopleQueryKey, usePeople, useShowRetired } from '../people/people';
import { claimsPath, claimsQueryKey, useClaims, useSelectedClaims, type Claim } from './claims';

// Moves every selected claim to the chosen person with one button, in list order: the people as
// listed, newest claim first within a person. Each is moved as its own Move button would, by the
// signed-in user (the server refuses what it refuses there, and that claim is left as it is); the
// counts of moved and refused show once all are done, and are gone while it runs.
function BulkReassign() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const [showRetired] = useShowRetired();
  const { selected, clear } = useSelectedClaims();
  const queryClient = useQueryClient();
  const [moveTo, setMoveTo] = useState('');
  const [result, setResult] = useState<{ reassigned: number; refused: number } | null>(null);
  const busy = useRef(false);
  if (!user || !sheet) return null;

  const all = people.data ?? [];
  const target = all.find((p) => String(p.id) === moveTo) ?? all[0];

  const run = async () => {
    if (busy.current || !target) return;
    busy.current = true;
    setResult(null);
    try {
      const listed = all.filter((p) => showRetired || p.retiredFrom === null);
      const order = listed.flatMap((p) => (claims.data ?? []).filter((c) => c.personId === p.id && selected.has(c.id)));
      let reassigned = 0;
      let refused = 0;
      for (const c of order) {
        try {
          await putJson<Claim>(`${claimsPath(sheet)}/${c.id}/person/${target.id}`, undefined, user);
          reassigned++;
        } catch {
          refused++;
        }
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet) }),
      ]);
      clear();
      setResult({ reassigned, refused });
    } finally {
      busy.current = false;
    }
  };

  return (
    <div>
      <select
        data-testid="bulk-reassign-to"
        value={target ? String(target.id) : ''}
        onChange={(e) => { setMoveTo(e.target.value); setResult(null); }}
      >
        {all.map((p) => (
          <option key={p.id} value={String(p.id)}>{p.name}</option>
        ))}
      </select>{' '}
      <button type="button" data-testid="bulk-reassign" disabled={!target} onClick={() => void run()}>
        Move selected claims
      </button>
      {result && (
        <>
          {' '}Moved <span data-testid="bulk-reassigned">{result.reassigned}</span>, refused{' '}
          <span data-testid="bulk-reassign-refused">{result.refused}</span>
        </>
      )}
    </div>
  );
}

export const contribution = HomeMain.fill({ order: 23, Component: BulkReassign });
