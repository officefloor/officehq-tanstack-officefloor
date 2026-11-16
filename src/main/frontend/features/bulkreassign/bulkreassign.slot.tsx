import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { claimsKey } from '../claims/claims';
import { useSelectedClaims } from '../claims/selection';
import { usePeople } from '../people/people';

type Result = { reassigned: number; refused: number };

/** Moves the selected claims to the chosen person together by the signed-in user: each is moved in list order (people
 *  in order, a person's claims newest first) by the rules of the single move; each that cannot is left as it is. Shows
 *  how many were moved and how many refused; the counts are cleared while a new press runs. */
function BulkReassign({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId);
  const [selected, setSelected] = useSelectedClaims();
  const [to, setTo] = useState('');
  const reassign = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-reassign`, {
        claimIds,
        personId: to === '' ? null : Number(to),
        by: user,
      }),
    onSuccess: async () => {
      setSelected([]);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <p>
      <select
        data-testid="bulk-reassign-to"
        aria-label="Move selected to"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          reassign.reset();
        }}
      >
        <option value="">Move to…</option>
        {(people.data ?? []).map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="bulk-reassign"
        disabled={reassign.isPending}
        onClick={() => {
          reassign.reset();
          reassign.mutate(selected);
        }}
      >
        Move selected ({selected.length})
      </button>
      {reassign.isSuccess && (
        <>
          {' '}
          Moved <span data-testid="bulk-reassigned">{reassign.data.reassigned}</span>, refused{' '}
          <span data-testid="bulk-reassign-refused">{reassign.data.refused}</span>
        </>
      )}
      {reassign.isError && <span data-testid="bulk-reassign-error"> Could not move the selected claims.</span>}
    </p>
  );
}

export const contribution = SheetPage.fill({ order: 27, Component: BulkReassign });
