import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { setSheetToday, sheetKey, type Sheet } from './api';

/** The sheet's today, shown and changeable. Every date on the sheet goes by it. */
export function SheetToday({ sheet, user }: { sheet: Sheet; user: string }) {
  const queryClient = useQueryClient();
  const [date, setDate] = useState('');
  const save = useMutation({
    mutationFn: (today: string) => setSheetToday(sheet.id, user, today),
    onSuccess: (updated) => {
      // Everything under the sheet may depend on its today: re-read it all.
      queryClient.setQueryData(sheetKey(sheet.id), updated);
      queryClient.invalidateQueries({
        queryKey: sheetKey(sheet.id),
        predicate: (q) => q.queryKey.length > 2,
      });
    },
  });

  const submitOnce = useSubmitOnce();

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const today = date.trim();
        submitOnce(() => {
          save.reset();
          return save.mutateAsync(today);
        });
      }}
    >
      Today <span data-testid="sheet-today">{sheet.today}</span>{' '}
      <input
        type="date"
        data-testid="sheet-today-input"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          save.reset();
        }}
      />{' '}
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set today
      </button>
      {save.isError && <span data-testid="sheet-error">That is not a date.</span>}
    </form>
  );
}
