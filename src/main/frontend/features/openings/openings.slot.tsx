import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { periodsKey } from '../periods/usePeriods';

/** An opening balances box: lines of name,period,amount (a person on the sheet by name or alias, a period on
 *  the sheet, an amount of two decimals, 0.00 or more), each adding its amount to the person's carried figure
 *  for the period. The server checks every line first: one refused line refuses the whole paste. Shows how
 *  many lines went through (gone while it runs). */
function ImportOpenings({ sheetId }: { sheetId: string }) {
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const importing = useMutation({
    mutationFn: (t: string) =>
      postJson<{ set: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/opening-import`, { text: t }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });
  return (
    <form
      data-testid="opening-import-form"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          importing.reset();
          return importing.mutateAsync(text);
        });
      }}
    >
      <label>
        Opening balances (name,period,amount per line){' '}
        <textarea
          data-testid="opening-import"
          rows={3}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            importing.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="opening-import-submit" disabled={importing.isPending}>
        Paste opening balances
      </button>
      {importing.isSuccess && (
        <>
          {' '}
          Set <strong data-testid="opening-import-done">{importing.data.set}</strong>
        </>
      )}
      {importing.isError && <p data-testid="opening-error">{importing.error.message}</p>}
    </form>
  );
}

export const contribution = SheetBody.fill({
  order: 11.5,
  Component: ({ sheetId }) => <ImportOpenings sheetId={sheetId} />,
});
