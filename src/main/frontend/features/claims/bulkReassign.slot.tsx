import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleQuery } from '../people/peopleQuery';
import { claimsKey, claimsQuery } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useSelectedClaims } from './useSelectedClaims';

type Result = { reassigned: number; refused: number };

/**
 * Moves every ticked claim to the chosen person as the signed-in user, one at a time in list order (people in
 * their list order, newest claim first within a person), by the same rules as moving one claim (each writes its
 * own CLAIM_REASSIGNED and rollup lines). A claim the server refuses to move is left as it is and counted as
 * refused; with no person chosen, every ticked claim is refused.
 */
function BulkReassign({ user, sheetId }: { user: string; sheetId: string }) {
  const queryClient = useQueryClient();
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const [selected, setSelected] = useSelectedClaims();
  const [to, setTo] = useState('');
  const [result, setResult] = useState<Result | undefined>();
  const once = useSubmitOnce();

  const ordered = people.flatMap((p) => claims.filter((c) => c.personId === p.id && selected.includes(c.id)));

  const reassign = useMutation({
    mutationFn: async ({ targets, personId }: { targets: Claim[]; personId: number | null }): Promise<Result> => {
      let reassigned = 0;
      if (personId !== null) {
        for (const c of targets) {
          try {
            await putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/person`, { personId }, user);
            reassigned++;
          } catch {
            // refused: left as it is
          }
        }
      }
      return { reassigned, refused: targets.length - reassigned };
    },
    onSuccess: async (r) => {
      setSelected([]);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      setResult(r);
    },
  });

  return (
    <section data-testid="bulk-reassign-panel">
      <select
        data-testid="bulk-reassign-to"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          setResult(undefined);
        }}
      >
        <option value="">Move to…</option>
        {people.map((p) => (
          <option key={p.id} value={p.id}>
            {p.name}
          </option>
        ))}
      </select>
      <button
        type="button"
        data-testid="bulk-reassign"
        disabled={reassign.isPending}
        onClick={() => {
          setResult(undefined);
          once(() => reassign.mutateAsync({ targets: ordered, personId: to ? Number(to) : null }));
        }}
      >
        Move selected
      </button>
      {result && (
        <>
          {' '}
          Moved <span data-testid="bulk-reassigned">{result.reassigned}</span>, refused{' '}
          <span data-testid="bulk-reassign-refused">{result.refused}</span>
        </>
      )}
    </section>
  );
}

export const contribution = SheetPanel.fill({
  order: 23,
  Component: ({ user, sheetId }) => <BulkReassign key={sheetId} user={user} sheetId={sheetId} />,
});
