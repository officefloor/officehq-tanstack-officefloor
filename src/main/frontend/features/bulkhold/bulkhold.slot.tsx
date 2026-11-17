import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useSelectedPeople } from '../bulkgrade/selection';

type Result = { held: number };

/** Holds the selected people together from a from-date (blank: the sheet's today) until a to-date (blank: open-ended):
 *  each, in list order, is held exactly as the row's hold would. Shows how many were held; the count is cleared while a
 *  new press runs. A refused bulk hold shows the error and changes nothing. */
function BulkHold({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useSelectedPeople();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const submit = useSubmitOnce();
  const hold = useMutation({
    mutationFn: (personIds: number[]) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-hold`, {
        personIds,
        from: from.trim(),
        to: to.trim(),
        by: user,
      }),
    onSuccess: async () => {
      setSelected([]);
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onSettled: submit.end,
  });

  return (
    <p data-testid="bulk-hold-bar">
      Hold selected people from{' '}
      <input
        data-testid="bulk-hold-from"
        placeholder="from yyyy-mm-dd"
        value={from}
        onChange={(e) => {
          setFrom(e.target.value);
          hold.reset();
        }}
      />{' '}
      to{' '}
      <input
        data-testid="bulk-hold-to"
        placeholder="to yyyy-mm-dd"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          hold.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-hold"
        disabled={hold.isPending}
        onClick={() => {
          if (!submit.begin()) return;
          hold.reset();
          hold.mutate(selected);
        }}
      >
        Hold ({selected.length})
      </button>
      {hold.isSuccess && (
        <>
          {' '}
          Held <span data-testid="bulk-held">{hold.data.held}</span>
        </>
      )}
      {hold.isError && <span data-testid="person-error"> Could not hold the selected people.</span>}
    </p>
  );
}

export const contribution = SheetPage.fill({ order: 11.6, Component: BulkHold });
