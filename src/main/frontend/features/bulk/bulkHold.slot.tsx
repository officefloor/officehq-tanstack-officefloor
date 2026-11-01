import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/usePeople';
import { useSheetToday } from '../sheet/useSheet';
import { useSelectedPeople } from './useSelectedPeople';

/**
 * Hold the ticked people together, from a date (blank: the sheet's today) to an optional date
 * (blank: open-ended). Each, in list order, gets the hold as the row's hold would. Shows how many
 * were held; the count goes while the next press runs.
 */
function BulkHold({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const today = useSheetToday(sheet);
  const [selected, setSelected] = useSelectedPeople();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const bulk = useMutation({
    mutationFn: (personIds: number[]) =>
      postJson<{ held: number }>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-hold`, {
        personIds,
        from: from.trim(),
        to: to.trim(),
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
    <section data-testid="bulk-hold-panel">
      <label>
        Hold from{' '}
        <input
          data-testid="bulk-hold-from"
          placeholder={today || 'yyyy-mm-dd'}
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            bulk.reset();
          }}
        />
      </label>{' '}
      <label>
        To{' '}
        <input
          data-testid="bulk-hold-to"
          placeholder="open-ended"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            bulk.reset();
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="bulk-hold"
        disabled={bulk.isPending}
        onClick={() => {
          bulk.reset();
          bulk.mutate(selected);
        }}
      >
        Hold selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <span>
          {' '}
          Held <strong data-testid="bulk-held">{bulk.data.held}</strong>
        </span>
      )}
      {bulk.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          {selected.length === 0 ? 'Select the people to hold.' : 'Could not hold the selected people.'}
        </span>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 17.6, Component: BulkHold });
