import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { claimsKey, useClaims } from './claims';
import { clearSelected, useSelectedClaims } from './selection';

type BulkRedateResult = { redated: number; refused: number };

/** The date entry and button that re-date every selected claim together to that date, by the signed-in user.
 *  The server re-dates each that can be re-dated, in list order and by the rules of re-dating one, and leaves the
 *  rest as they are; the bar then shows how many were re-dated and how many refused. */
function BulkRedate({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const claims = useClaims(sheetId).data ?? [];
  const chosen = useSelectedClaims(sheetId);
  const ids = claims.filter((c) => chosen.has(c.id)).map((c) => c.id);
  const [date, setDate] = useState('');
  const [result, setResult] = useState<BulkRedateResult | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const redate = useMutation({
    mutationFn: (body: { claimIds: number[]; date: string }) =>
      postJson<BulkRedateResult>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-redate`, { user, ...body }),
    onSuccess: async (r) => {
      clearSelected(sheetId);
      setResult(r);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not re-date the selected claims.'),
    onSettled: once.done,
  });

  return (
    <p>
      <input
        data-testid="bulk-redate-date"
        placeholder="Re-date to yyyy-mm-dd"
        value={date}
        onChange={(e) => {
          setError('');
          setDate(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-redate"
        disabled={redate.isPending}
        onClick={() => {
          setError('');
          setResult(null);
          if (ids.length === 0) {
            setError('Select the claims to re-date.');
            return;
          }
          if (date === '') {
            setError('Give the new date for the selected claims.');
            return;
          }
          if (!once.start()) return;
          redate.mutate({ claimIds: ids, date });
        }}
      >
        Re-date selected
      </button>
      {result && (
        <>
          {' '}Re-dated <span data-testid="bulk-redated">{result.redated}</span>, refused{' '}
          <span data-testid="bulk-redate-refused">{result.refused}</span>
        </>
      )}
      {error && (
        <span data-testid="bulk-redate-error" role="alert">
          {' '}{error}
        </span>
      )}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 13.6, Component: BulkRedate });
