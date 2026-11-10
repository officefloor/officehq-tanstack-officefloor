import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { peopleKey, usePeople } from '../people/people';
import { useUser } from '../session/session';
import { claimsKey } from './claims';
import { clearSelectedClaims, useSelectedClaims } from './selection';

type Result = { reassigned: number; refused: number };

// Moves the ticked claims to the chosen person together, as the signed-in user. The server moves
// each that can be moved in list order (people in list order, each person's claims newest first) by
// the rules of moving one, and leaves the rest as they are; the bar shows how many were moved and
// how many refused. The counts are cleared while it runs.
function BulkReassign({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedClaims(sheetId);
  const people = usePeople(sheetId).data ?? [];
  const [user] = useUser();
  const [to, setTo] = useState('');
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState('');

  const once = useOnce();
  const reassign = useMutation({
    mutationFn: ({ claimIds, personId }: { claimIds: string[]; personId: string }) =>
      postJson<Result>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/bulk-reassign`,
        { claimIds, personId },
        user,
      ),
    onSuccess: (r) => {
      setResult(r);
      clearSelectedClaims(sheetId);
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: () => setError('The selected claims could not be moved.'),
  });

  const press = () => {
    setResult(null);
    setError('');
    const claimIds = [...selected];
    once((done) => reassign.mutate({ claimIds, personId: to }, { onSettled: done }));
  };

  return (
    <section data-testid="bulk-reassign-claims">
      <select
        data-testid="bulk-reassign-to"
        aria-label="Move selected to"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          setError('');
        }}
      >
        <option value="">Move to…</option>
        {people.map((p) => (
          <option key={p.id} value={p.id}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button type="button" data-testid="bulk-reassign" onClick={press} disabled={reassign.isPending}>
        Move selected ({selected.size})
      </button>
      {result !== null && (
        <>
          {' '}Moved <span data-testid="bulk-reassigned">{result.reassigned}</span>, refused{' '}
          <span data-testid="bulk-reassign-refused">{result.refused}</span>
        </>
      )}
      {error && (
        <p role="alert" data-testid="bulk-reassign-error">
          {error}
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6.5, Component: BulkReassign });
