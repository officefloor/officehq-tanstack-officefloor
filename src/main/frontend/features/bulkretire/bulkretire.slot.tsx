import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useSelectedPeople } from '../bulkgrade/selection';

type Result = { retired: number };

/** Retires the selected people together from a date (blank: the sheet's today): each, in list order, is retired exactly
 *  as the row's retire would. Shows how many were retired; the count is cleared while a new press runs. A refused bulk
 *  retire shows the error and changes nothing. */
function BulkRetire({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useSelectedPeople();
  const [from, setFrom] = useState('');
  const submit = useSubmitOnce();
  const retire = useMutation({
    mutationFn: (personIds: number[]) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-retire`, {
        personIds,
        from: from.trim(),
        by: user,
      }),
    onSuccess: async () => {
      setSelected([]);
      setFrom('');
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onSettled: submit.end,
  });

  return (
    <p data-testid="bulk-retire-bar">
      Retire selected people from{' '}
      <input
        data-testid="bulk-retire-date"
        placeholder="yyyy-mm-dd"
        value={from}
        onChange={(e) => {
          setFrom(e.target.value);
          retire.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-retire"
        disabled={retire.isPending}
        onClick={() => {
          if (!submit.begin()) return;
          retire.reset();
          retire.mutate(selected);
        }}
      >
        Retire ({selected.length})
      </button>
      {retire.isSuccess && (
        <>
          {' '}
          Retired <span data-testid="bulk-retired">{retire.data.retired}</span>
        </>
      )}
      {retire.isError && <span data-testid="person-error"> Could not retire the selected people.</span>}
    </p>
  );
}

export const contribution = SheetPage.fill({ order: 11.5, Component: BulkRetire });
