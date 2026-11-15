import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { bulkRedateClaims, claimsKey } from './api';

/** A date entry and a button that re-dates the selected claims to it together, and how many it re-dated
 *  and refused. The counts are cleared while it runs; the selection is cleared once it is done. */
export function BulkRedate({ sheetId, user, selected, onDone }: {
  sheetId: string; user: string; selected: number[]; onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const runOnce = useSubmitOnce();
  const [date, setDate] = useState('');
  const bulk = useMutation({
    mutationFn: (v: { ids: number[]; date: string }) => bulkRedateClaims(sheetId, user, v.ids, v.date),
    onSuccess: () => {
      onDone();
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <div>
      <input type="date" data-testid="bulk-redate-date" value={date}
        onChange={(e) => {
          setDate(e.target.value);
          bulk.reset();
        }} />{' '}
      <button type="button" data-testid="bulk-redate" disabled={bulk.isPending}
        onClick={() => {
          const ids = [...selected];
          const to = date.trim();
          runOnce(() => {
            bulk.reset();
            return bulk.mutateAsync({ ids, date: to });
          });
        }}>
        Re-date selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}Re-dated <span data-testid="bulk-redated">{bulk.data.redated}</span>,{' '}
          refused <span data-testid="bulk-redate-refused">{bulk.data.refused}</span>
        </>
      )}
      {bulk.isError && <span data-testid="bulk-redate-error">The selected claims could not be re-dated.</span>}
    </div>
  );
}
