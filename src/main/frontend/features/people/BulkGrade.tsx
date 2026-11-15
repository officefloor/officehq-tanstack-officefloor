import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { claimsKey } from '../claims/api';
import { GRADES, bulkGradePeople, peopleKey } from './api';

/** A grade, a from-date (blank: the sheet's today) and a button that gives the selected people that
 *  grade together, and how many it changed. The count is cleared while it runs; the selection is
 *  cleared once it is done. A refusal shows until the form is changed or submitted again. */
export function BulkGrade({ sheetId, user, selected, onDone }: {
  sheetId: string; user: string; selected: number[]; onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const runOnce = useSubmitOnce();
  const [grade, setGrade] = useState<string>('A');
  const [from, setFrom] = useState('');
  const bulk = useMutation({
    mutationFn: (v: { ids: number[]; grade: string; from: string }) => bulkGradePeople(sheetId, user, v.ids, v.grade, v.from),
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
        Grade{' '}
        <select data-testid="bulk-grade" value={grade} onChange={(e) => { setGrade(e.target.value); bulk.reset(); }}>
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
      </label>{' '}
      <label>
        from{' '}
        <input type="date" data-testid="bulk-grade-from" value={from} onChange={(e) => { setFrom(e.target.value); bulk.reset(); }} />
      </label>{' '}
      <button type="button" data-testid="bulk-grade-apply" disabled={bulk.isPending}
        onClick={() => {
          const v = { ids: [...selected], grade, from };
          runOnce(() => {
            bulk.reset();
            return bulk.mutateAsync(v);
          });
        }}>
        Grade selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}Changed <span data-testid="bulk-graded">{bulk.data.graded}</span>
        </>
      )}
      {bulk.isError && <span data-testid="person-error">The selected people could not be graded: select someone, and a from-date (if any) must be a date.</span>}
    </div>
  );
}
