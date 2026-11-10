import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { usePersonSelection } from '../bulkgrade/usePersonSelection';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/usePeople';
import { poolsKey } from '../pools/usePools';

/** Hold the selected people together from a from-date (blank: the sheet's today) to an optional to-date
 *  (blank: open-ended): the server holds each, in list order, exactly as the row's hold would; the bar
 *  shows how many were held (gone while it runs). */
function BulkHold({ sheetId }: { sheetId: string }) {
  const selection = usePersonSelection(sheetId);
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const bulk = useMutation({
    mutationFn: (f: { personIds: number[]; from: string; to: string }) =>
      postJson<{ held: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-hold`, f),
    onSuccess: () => selection.clear(),
    onSettled: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      ]);
    },
  });
  return (
    <p data-testid="bulk-hold-bar">
      <label>
        Hold selected from{' '}
        <input
          type="text"
          placeholder="yyyy-mm-dd"
          data-testid="bulk-hold-from"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            bulk.reset();
          }}
        />
      </label>{' '}
      <label>
        to{' '}
        <input
          type="text"
          placeholder="yyyy-mm-dd"
          data-testid="bulk-hold-to"
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
        onClick={() =>
          submitOnce(() => {
            bulk.reset();
            return bulk.mutateAsync({ personIds: [...selection.ids], from: from.trim(), to: to.trim() });
          })
        }
      >
        Hold selected ({selection.ids.size})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Held <strong data-testid="bulk-held">{bulk.data.held}</strong>
        </>
      )}
      {bulk.isError && <span data-testid="person-error">{bulk.error.message}</span>}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 13.5, Component: BulkHold });
