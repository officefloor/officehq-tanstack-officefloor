import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { peopleKey } from '../people/people';
import { claimsKey, useSelectedClaims } from './claims';

type BulkRedateResult = { redated: number; refused: number };

// Re-dates the selected claims together: the server re-dates each in list order by the rules of a
// single re-dating and refuses the rest, and the counts show once it answers (gone while it runs).
// Re-dating claims re-reads the people too: their figures follow the claims.
function BulkRedate() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [selected, setSelected] = useSelectedClaims();
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();

  const bulk = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<BulkRedateResult>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-redate`, {
        claimIds: claimIds.map(String),
        date,
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
      <input
        type="date"
        data-testid="bulk-redate-date"
        aria-label="Re-date selected to"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          bulk.reset();
        }}
      />{' '}
      <button type="button" data-testid="bulk-redate" disabled={bulk.isPending} onClick={() => submit(selected)}>
        Re-date selected
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Re-dated <strong data-testid="bulk-redated">{bulk.data.redated}</strong>, refused{' '}
          <strong data-testid="bulk-redate-refused">{bulk.data.refused}</strong>
        </>
      )}
      {bulk.isError && (
        <span data-testid="bulk-redate-error" role="alert">
          {' '}
          {bulk.error instanceof HttpError && bulk.error.reason ? `${bulk.error.reason}.` : 'The bulk re-date was refused.'}
        </span>
      )}
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 24.5, Component: BulkRedate });
