import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleQuery } from '../people/peopleQuery';
import { claimsKey, claimsQuery } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useSelectedClaims } from './useSelectedClaims';
import { setClaimError } from './useClaimError';

type Result = { opened: number; refused: number };

/**
 * Opens every ticked claim, one at a time in list order (people in their list order, newest claim
 * first within a person), so each is numbered in that order. A claim the server refuses to open is
 * left as it is and counted as refused; a refusal the server explains (e.g. a claim above the note threshold
 * with an empty note) shows on the claim.
 */
function BulkOpen({ user, sheetId }: { user: string; sheetId: string }) {
  const queryClient = useQueryClient();
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const [selected, setSelected] = useSelectedClaims();
  const [result, setResult] = useState<Result | undefined>();
  const once = useSubmitOnce();

  const ordered = people.flatMap((p) => claims.filter((c) => c.personId === p.id && selected.includes(c.id)));

  const open = useMutation({
    mutationFn: async (targets: Claim[]): Promise<Result> => {
      let opened = 0;
      for (const c of targets) {
        setClaimError(c.id, '');
        try {
          await putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/status/OPEN`, {}, user);
          opened++;
        } catch (e) {
          // refused: left as it is
          if (e instanceof RefusedError) setClaimError(c.id, e.message);
        }
      }
      return { opened, refused: targets.length - opened };
    },
    onSuccess: async (r) => {
      setSelected([]);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      setResult(r);
    },
  });

  return (
    <section data-testid="bulk-open-panel">
      <button
        type="button"
        data-testid="bulk-open"
        disabled={open.isPending}
        onClick={() => {
          setResult(undefined);
          once(() => open.mutateAsync(ordered));
        }}
      >
        Open selected
      </button>
      {result && (
        <>
          {' '}
          Opened <span data-testid="bulk-opened">{result.opened}</span>, refused{' '}
          <span data-testid="bulk-refused">{result.refused}</span>
        </>
      )}
    </section>
  );
}

export const contribution = SheetPanel.fill({
  order: 21,
  Component: ({ user, sheetId }) => <BulkOpen key={sheetId} user={user} sheetId={sheetId} />,
});
