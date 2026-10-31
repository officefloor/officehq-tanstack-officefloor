import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/usePeople';
import { useSelectedClaims } from './useSelectedClaims';

/**
 * Re-date the ticked claims together to one date. Each is re-dated in list order (people in list
 * order, each person's newest claim first) by the rules of re-dating one claim; each that cannot is
 * left as it is. Shows how many were re-dated and how many refused; the counts go while the next
 * press runs.
 */
function BulkRedate({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useSelectedClaims();
  const [date, setDate] = useState('');
  const bulk = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<{ redated: number; refused: number }>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-redate`, {
        claimIds,
        date: date.trim(),
        user,
      }),
    onSuccess: () => setSelected([]),
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]),
  });
  return (
    <section data-testid="bulk-redate-panel">
      <label>
        Re-date to{' '}
        <input
          data-testid="bulk-redate-date"
          placeholder="yyyy-mm-dd"
          value={date}
          onChange={(e) => {
            setDate(e.target.value);
            bulk.reset();
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="bulk-redate"
        disabled={bulk.isPending}
        onClick={() => {
          bulk.reset();
          bulk.mutate(selected);
        }}
      >
        Re-date selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <span>
          {' '}
          Re-dated <strong data-testid="bulk-redated">{bulk.data.redated}</strong>, refused{' '}
          <strong data-testid="bulk-redate-refused">{bulk.data.refused}</strong>
        </span>
      )}
      {bulk.isError && (
        <span data-testid="bulk-redate-error" role="alert">
          {' '}
          {selected.length === 0
            ? 'Select the claims to re-date.'
            : date.trim() === ''
              ? 'Enter the date to re-date the selected claims to.'
              : 'Could not re-date the selected claims.'}
        </span>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 16.7, Component: BulkRedate });
