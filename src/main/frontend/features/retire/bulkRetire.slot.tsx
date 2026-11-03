import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { usePersonSelection } from '../bulkgrade/usePersonSelection';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/usePeople';

/** Retire the selected people together from a date (blank: the sheet's today): the server retires each, in
 *  list order, exactly as the row's retire would; the bar shows how many were retired (gone while it runs). */
function BulkRetire({ sheetId }: { sheetId: string }) {
  const selection = usePersonSelection(sheetId);
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const [date, setDate] = useState('');
  const bulk = useMutation({
    mutationFn: (f: { personIds: number[]; date: string }) =>
      postJson<{ retired: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-retire`, f),
    onSuccess: () => selection.clear(),
    onSettled: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });
  return (
    <p data-testid="bulk-retire-bar">
      <label>
        Retire selected from{' '}
        <input
          type="text"
          placeholder="yyyy-mm-dd"
          data-testid="bulk-retire-date"
          value={date}
          onChange={(e) => {
            setDate(e.target.value);
            bulk.reset();
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="bulk-retire"
        disabled={bulk.isPending}
        onClick={() =>
          submitOnce(() => {
            bulk.reset();
            return bulk.mutateAsync({ personIds: [...selection.ids], date: date.trim() });
          })
        }
      >
        Retire selected ({selection.ids.size})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Retired <strong data-testid="bulk-retired">{bulk.data.retired}</strong>
        </>
      )}
      {bulk.isError && <span data-testid="person-error">{bulk.error.message}</span>}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 13, Component: BulkRetire });
