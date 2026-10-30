import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { sheetTodayKey, useSheetArchived, useSheetToday, type SheetToday as SheetTodayDto } from './useSheetToday';

// Shows the active sheet's today and changes it. A blank date resets it to the real date.
// Also shows whether the sheet is archived, and archives it (a steward only).
export function SheetToday() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const today = useSheetToday(sheet);
  const archived = useSheetArchived(sheet);
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
  const archive = useMutation({
    mutationFn: () =>
      postJson<SheetTodayDto>(`/api/sheets/${encodeURIComponent(sheet)}/archive`, { user }),
    onSuccess: (saved) => {
      queryClient.setQueryData(sheetTodayKey(saved.sheet), saved);
    },
  });
  return (
    <form
      data-testid="sheet-today-form"
      onSubmit={(e) => {
        e.preventDefault();
        archive.reset();
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
          archive.reset();
        }}
      />
      <button type="submit" data-testid="sheet-today-save">Set today</button>
      {' '}Archived: <span data-testid="sheet-archived">{archived === undefined ? '' : archived ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="sheet-archive"
        onClick={() => {
          save.reset();
          archive.reset();
          submitOnce(() => archive.mutateAsync());
        }}
      >
        Archive sheet
      </button>
      {save.isError && <span data-testid="sheet-error" role="alert"> Could not change today.</span>}
      {archive.isError && <span data-testid="sheet-error" role="alert"> Could not archive the sheet.</span>}
    </form>
  );
}
