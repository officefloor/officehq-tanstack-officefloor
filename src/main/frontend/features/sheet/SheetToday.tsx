import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { sheetTodayKey, useSheetToday, type SheetToday as SheetTodayDto } from './useSheetToday';

// Shows the active sheet's today and changes it. A blank date resets it to the real date.
export function SheetToday() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const today = useSheetToday(sheet);
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (value: string) =>
      postJson<SheetTodayDto>(`/api/sheets/${encodeURIComponent(sheet)}/today`, { today: value, user }),
    onSuccess: (saved) => {
      queryClient.setQueryData(sheetTodayKey(saved.sheet), saved);
      // Everything on the sheet may depend on its date: re-read it all.
      void queryClient.invalidateQueries({ queryKey: ['sheet', saved.sheet] });
    },
  });
  return (
    <form
      data-testid="sheet-today-form"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => save.mutateAsync(date.trim()));
      }}
    >
      Today: <span data-testid="sheet-today">{today ?? ''}</span>{' '}
      <input
        type="date"
        data-testid="sheet-today-input"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          save.reset();
        }}
      />
      <button type="submit" data-testid="sheet-today-save">Set today</button>
      {save.isError && <span data-testid="sheet-error" role="alert"> Could not change today.</span>}
    </form>
  );
}
