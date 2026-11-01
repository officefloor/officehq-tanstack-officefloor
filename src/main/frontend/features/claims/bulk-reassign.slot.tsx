import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { usePeople } from '../people/people';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey, claimsPath } from './claims';
import type { Claim } from './claims';
import { clearSelectedClaims, useSelectedClaims } from './selection';

type BulkReassigned = { reassigned: number; refused: number; claims: Claim[] };

// Moves the selected claims together to the person picked beside the button: the server moves each in list order
// (people in the order they were added, newest claim first) by the rules of reassigning one, and leaves the rest as
// they are. Shows how many of each.
function BulkReassign({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const [moveTo, setMoveTo] = useState('');
  const reassign = useMutation({
    mutationFn: ({ ids, to }: { ids: number[]; to: number }) =>
      postJson<BulkReassigned>(byUser(`${claimsPath(sheet)}/bulk-reassign`, user), { claims: ids, to }),
    onSuccess: (result) => {
      queryClient.setQueryData(claimsKey(sheet), result.claims);
      clearSelectedClaims(sheet);
    },
  });
  const act = useSubmitOnce(reassign);

  return (
    <p>
      <select
        data-testid="bulk-reassign-to"
        value={moveTo}
        onChange={(e) => {
          setMoveTo(e.target.value);
          reassign.reset();
        }}
      >
        <option value="">move to…</option>
        {people.map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="bulk-reassign"
        disabled={reassign.isPending || moveTo === ''}
        onClick={() => act({ ids: [...selected], to: Number(moveTo) })}
      >
        Reassign selected
      </button>
      {reassign.isSuccess && (
        <>
          {' '}Reassigned <strong data-testid="bulk-reassigned">{reassign.data.reassigned}</strong>, refused{' '}
          <strong data-testid="bulk-reassign-refused">{reassign.data.refused}</strong>
        </>
      )}
      {reassign.isError && (
        <span data-testid="bulk-reassign-error" role="alert">
          {' '}The bulk reassign failed.
        </span>
      )}
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 22.5, Component: BulkReassign });
