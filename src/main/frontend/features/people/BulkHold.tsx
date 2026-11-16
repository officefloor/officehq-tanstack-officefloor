import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { claimsKey } from '../claims/api';
import { bulkHoldPeople, peopleKey } from './api';

/** A from-date (blank: the sheet's today), a to-date (blank: open-ended) and a button that holds the
 *  selected people together, and how many it held. The count is cleared while it runs; the selection
 *  is cleared once it is done. A refusal shows until the form is changed or submitted again. */
export function BulkHold({ sheetId, user, selected, onDone }: {
  sheetId: string; user: string; selected: number[]; onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const runOnce = useSubmitOnce();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const bulk = useMutation({
    mutationFn: (v: { ids: number[]; from: string; to: string }) => bulkHoldPeople(sheetId, user, v.ids, v.from, v.to),
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
        Hold from{' '}
        <input type="date" data-testid="bulk-hold-from" value={from} onChange={(e) => { setFrom(e.target.value); bulk.reset(); }} />
      </label>{' '}
      <label>
        to{' '}
        <input type="date" data-testid="bulk-hold-to" value={to} onChange={(e) => { setTo(e.target.value); bulk.reset(); }} />
      </label>{' '}
      <button type="button" data-testid="bulk-hold" disabled={bulk.isPending}
        onClick={() => {
          const v = { ids: [...selected], from, to };
          runOnce(() => {
            bulk.reset();
            return bulk.mutateAsync(v);
          });
        }}>
        Hold selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}Held <span data-testid="bulk-held">{bulk.data.held}</span>
        </>
      )}
      {bulk.isError && <span data-testid="person-error">The selected people could not be held: select someone, the dates must be dates, and the to-date not before the from-date.</span>}
    </div>
  );
}
