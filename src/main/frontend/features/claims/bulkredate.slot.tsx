import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { peopleKey } from '../people/people';
import { useUser } from '../session/session';
import { claimsKey } from './claims';
import { clearSelectedClaims, useSelectedClaims } from './selection';

type Result = { redated: number; refused: number };

// Re-dates the ticked claims to one date together, as the signed-in user. The server re-dates each
// that can be re-dated in list order (people in list order, each person's claims newest first) by
// the rules of re-dating one, and leaves the rest as they are; the bar shows how many were re-dated
// and how many refused. The counts are cleared while it runs.
function BulkRedate({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedClaims(sheetId);
  const [user] = useUser();
  const [date, setDate] = useState('');
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState('');

  const once = useOnce();
  const redate = useMutation({
    mutationFn: ({ claimIds, date }: { claimIds: string[]; date: string }) =>
      postJson<Result>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/bulk-redate`,
        { claimIds, date },
        user,
      ),
    onSuccess: (r) => {
      setResult(r);
      clearSelectedClaims(sheetId);
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: () => setError('The selected claims could not be re-dated.'),
  });

  const press = () => {
    setResult(null);
    setError('');
    const claimIds = [...selected];
    const d = date.trim();
    if (d === '') {
      setError('Choose the new date for the selected claims.');
      return;
    }
    once((done) => redate.mutate({ claimIds, date: d }, { onSettled: done }));
  };

  return (
    <section data-testid="bulk-redate-claims">
      <input
        type="date"
        data-testid="bulk-redate-date"
        aria-label="Re-date selected to"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          setError('');
        }}
      />{' '}
      <button type="button" data-testid="bulk-redate" onClick={press} disabled={redate.isPending}>
        Re-date selected ({selected.size})
      </button>
      {result !== null && (
        <>
          {' '}Re-dated <span data-testid="bulk-redated">{result.redated}</span>, refused{' '}
          <span data-testid="bulk-redate-refused">{result.refused}</span>
        </>
      )}
      {error && (
        <p role="alert" data-testid="bulk-redate-error">
          {error}
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6.6, Component: BulkRedate });
