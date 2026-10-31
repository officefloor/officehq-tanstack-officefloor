import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/usePeople';
import { useSheetToday } from '../sheet/useSheet';
import { useSelectedPeople } from './useSelectedPeople';

/**
 * Retire the ticked people together, from a date (blank: the sheet's today). Each, in list order,
 * is retired as the row's retire would. Shows how many were retired; the count goes while the next
 * press runs.
 */
function BulkRetire({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const today = useSheetToday(sheet);
  const [selected, setSelected] = useSelectedPeople();
  const [from, setFrom] = useState('');
  const bulk = useMutation({
    mutationFn: (personIds: number[]) =>
      postJson<{ retired: number }>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-retire`, {
        personIds,
        from: from.trim(),
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
    <section data-testid="bulk-retire-panel">
      <label>
        Retire from{' '}
        <input
          data-testid="bulk-retire-date"
          placeholder={today || 'yyyy-mm-dd'}
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            bulk.reset();
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="bulk-retire"
        disabled={bulk.isPending}
        onClick={() => {
          bulk.reset();
          bulk.mutate(selected);
        }}
      >
        Retire selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <span>
          {' '}
          Retired <strong data-testid="bulk-retired">{bulk.data.retired}</strong>
        </span>
      )}
      {bulk.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          {selected.length === 0 ? 'Select the people to retire.' : 'Could not retire the selected people.'}
        </span>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 17.5, Component: BulkRetire });
