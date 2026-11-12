import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { peopleKey, usePeople } from '../people/people';
import { claimsKey, useSelectedClaims } from './claims';

type BulkReassignResult = { reassigned: number; refused: number };

// Moves the selected claims to the chosen person together: the server moves each in list order by
// the rules of a single move and refuses the rest, and the counts show once it answers (gone while
// it runs). Moving claims re-reads the people too: their figures follow the claims.
function BulkReassign() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [selected, setSelected] = useSelectedClaims();
  const people = usePeople(sheet);
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();

  const bulk = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<BulkReassignResult>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-reassign`, {
        claimIds: claimIds.map(String),
        to,
        user,
      }),
    onSuccess: async () => {
      setSelected([]);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });
  const submit = useSubmitOnce(bulk);

  return (
    <p>
      <select
        data-testid="bulk-reassign-to"
        aria-label="Move selected to"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          bulk.reset();
        }}
      >
        <option value="">Move to…</option>
        {(people.data ?? []).map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button type="button" data-testid="bulk-reassign" disabled={bulk.isPending} onClick={() => submit(selected)}>
        Move selected
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Moved <strong data-testid="bulk-reassigned">{bulk.data.reassigned}</strong>, refused{' '}
          <strong data-testid="bulk-reassign-refused">{bulk.data.refused}</strong>
        </>
      )}
      {bulk.isError && (
        <span data-testid="bulk-reassign-error" role="alert">
          {' '}
          {bulk.error instanceof HttpError && bulk.error.reason ? `${bulk.error.reason}.` : 'The bulk move was refused.'}
        </span>
      )}
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 24, Component: BulkReassign });
