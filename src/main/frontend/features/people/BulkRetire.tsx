import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { claimsKey } from '../claims/api';
import { bulkRetirePeople, peopleKey } from './api';

/** A date (blank: the sheet's today) and a button that retires the selected people together, and
 *  how many it retired. The count is cleared while it runs; the selection is cleared once it is
 *  done. A refusal shows until the form is changed or submitted again. */
export function BulkRetire({ sheetId, user, selected, onDone }: {
  sheetId: string; user: string; selected: number[]; onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const runOnce = useSubmitOnce();
  const [from, setFrom] = useState('');
  const bulk = useMutation({
    mutationFn: (v: { ids: number[]; from: string }) => bulkRetirePeople(sheetId, user, v.ids, v.from),
    onSuccess: () => {
      onDone();
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });

  return (
    <div>
      <label>
        Retire from{' '}
        <input type="date" data-testid="bulk-retire-date" value={from} onChange={(e) => { setFrom(e.target.value); bulk.reset(); }} />
      </label>{' '}
      <button type="button" data-testid="bulk-retire" disabled={bulk.isPending}
        onClick={() => {
          const v = { ids: [...selected], from };
          runOnce(() => {
            bulk.reset();
            return bulk.mutateAsync(v);
          });
        }}>
        Retire selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}Retired <span data-testid="bulk-retired">{bulk.data.retired}</span>
        </>
      )}
      {bulk.isError && <span data-testid="person-error">The selected people could not be retired: select someone, and the date must be a date.</span>}
    </div>
  );
}
