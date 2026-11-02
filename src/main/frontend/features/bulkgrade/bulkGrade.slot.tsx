import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { claimsKey } from '../claims/useClaims';
import { GRADES, peopleKey } from '../people/usePeople';
import { usePersonSelection } from './usePersonSelection';

/** A tick on the person's row that selects them for a bulk grade. */
function PersonSelect({ sheetId, personId }: { sheetId: string; personId: number }) {
  const selection = usePersonSelection(sheetId);
  return (
    <td>
      <input
        type="checkbox"
        data-testid="person-select"
        checked={selection.ids.has(personId)}
        onChange={(e) => selection.toggle(personId, e.target.checked)}
      />
    </td>
  );
}

/** Give the selected people a grade together: the server gives each, in list order, the grade from the
 *  from-date (blank: the sheet's today) exactly as the person form would; the bar shows how many were
 *  changed (gone while it runs). */
function BulkGrade({ sheetId }: { sheetId: string }) {
  const selection = usePersonSelection(sheetId);
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const [grade, setGrade] = useState<string>('A');
  const [from, setFrom] = useState('');
  const bulk = useMutation({
    mutationFn: (f: { personIds: number[]; grade: string; from: string }) =>
      postJson<{ graded: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-grade`, f),
    onSuccess: () => selection.clear(),
    onSettled: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });
  return (
    <p data-testid="bulk-grade-bar">
      <label>
        Grade selected{' '}
        <select
          data-testid="bulk-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            bulk.reset();
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
      </label>{' '}
      <label>
        from{' '}
        <input
          type="date"
          data-testid="bulk-grade-from"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            bulk.reset();
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="bulk-grade-apply"
        disabled={bulk.isPending}
        onClick={() =>
          submitOnce(() => {
            bulk.reset();
            return bulk.mutateAsync({ personIds: [...selection.ids], grade, from });
          })
        }
      >
        Grade selected ({selection.ids.size})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Changed <strong data-testid="bulk-graded">{bulk.data.graded}</strong>
        </>
      )}
      {bulk.isError && <span data-testid="person-error">{bulk.error.message}</span>}
    </p>
  );
}

export const contributions = [
  PersonColumnHead.fill({ order: 1, Component: () => <th>Select</th> }),
  PersonCells.fill({ order: 1, Component: PersonSelect }),
  SheetBody.fill({ order: 12, Component: BulkGrade }),
];
