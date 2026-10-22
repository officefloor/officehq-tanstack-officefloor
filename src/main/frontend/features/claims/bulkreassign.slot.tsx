import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { usePeople } from '../people/people';
import { claimsKey, useClaims } from './claims';
import { clearSelected, useSelectedClaims } from './selection';

type BulkReassignResult = { reassigned: number; refused: number };

/** The person select and button that move every selected claim together to that person, by the signed-in user.
 *  The server moves each that can be moved, in list order and by the rules of moving one, and leaves the rest as
 *  they are; the bar then shows how many were moved and how many refused. */
function BulkReassign({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const claims = useClaims(sheetId).data ?? [];
  const people = usePeople(sheetId).data ?? [];
  const chosen = useSelectedClaims(sheetId);
  const ids = claims.filter((c) => chosen.has(c.id)).map((c) => c.id);
  const [moveTo, setMoveTo] = useState('');
  const [result, setResult] = useState<BulkReassignResult | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const reassign = useMutation({
    mutationFn: (body: { claimIds: number[]; personId: string }) =>
      postJson<BulkReassignResult>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-reassign`, { user, ...body }),
    onSuccess: async (r) => {
      clearSelected(sheetId);
      setResult(r);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not move the selected claims.'),
    onSettled: once.done,
  });

  return (
    <p>
      <select
        data-testid="bulk-reassign-to"
        value={moveTo}
        onChange={(e) => {
          setError('');
          setMoveTo(e.target.value);
        }}
      >
        <option value="">Move selected to…</option>
        {people.map((p) => (
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
          setError('');
          setResult(null);
          if (ids.length === 0) {
            setError('Select the claims to move.');
            return;
          }
          if (moveTo === '') {
            setError('Choose the person to move the selected claims to.');
            return;
          }
          if (!once.start()) return;
          reassign.mutate({ claimIds: ids, personId: moveTo });
        }}
      >
        Move selected
      </button>
      {result && (
        <>
          {' '}Moved <span data-testid="bulk-reassigned">{result.reassigned}</span>, refused{' '}
          <span data-testid="bulk-reassign-refused">{result.refused}</span>
        </>
      )}
      {error && (
        <span data-testid="bulk-reassign-error" role="alert">
          {' '}{error}
        </span>
      )}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 13.5, Component: BulkReassign });
