import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useSettings } from '../settings/useSettings';
import { sheetTodayKey, useSheetArchived, useSheetToday, type SheetToday as SheetTodayDto } from './useSheetToday';

// Shows the active sheet's today and changes it. A blank date resets it to the real date.
// Also shows whether the sheet is archived, and archives it (a steward only), and its parent sheet (nothing when none),
// and clones it to another id never used (a steward only), showing the id cloned to once done.
export function SheetToday() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const today = useSheetToday(sheet);
  const archived = useSheetArchived(sheet);
  const settings = useSettings(sheet);
  const [date, setDate] = useState('');
  const [cloneTo, setCloneTo] = useState('');
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
  const clone = useMutation({
    mutationFn: (to: string) =>
      postJson<{ sheet: string; toSheet: string }>(`/api/sheets/${encodeURIComponent(sheet)}/clone`, { toSheet: to, user }),
  });
  const resetAll = () => {
    save.reset();
    archive.reset();
    clone.reset();
  };
  const { reset: resetClone } = clone;
  // A clone made from another sheet is not this one's.
  useEffect(() => resetClone(), [sheet, resetClone]);
  return (
    <form
      data-testid="sheet-today-form"
      onSubmit={(e) => {
        e.preventDefault();
        archive.reset();
        clone.reset();
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
          resetAll();
        }}
      />
      <button type="submit" data-testid="sheet-today-save">Set today</button>
      {' '}Parent: <span data-testid="sheet-parent">{settings?.parent ?? ''}</span>
      {' '}Archived: <span data-testid="sheet-archived">{archived === undefined ? '' : archived ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="sheet-archive"
        onClick={() => {
          resetAll();
          submitOnce(() => archive.mutateAsync());
        }}
      >
        Archive sheet
      </button>
      {' '}Clone to:{' '}
      <input
        data-testid="sheet-clone-to"
        value={cloneTo}
        onChange={(e) => {
          setCloneTo(e.target.value);
          resetAll();
        }}
      />
      <button
        type="button"
        data-testid="sheet-clone"
        onClick={() => {
          resetAll();
          submitOnce(() => clone.mutateAsync(cloneTo.trim()));
        }}
      >
        Clone sheet
      </button>{' '}
      <span data-testid="sheet-cloned-to">{clone.data?.toSheet ?? ''}</span>
      {save.isError && <span data-testid="sheet-error" role="alert"> Could not change today.</span>}
      {archive.isError && <span data-testid="sheet-error" role="alert"> Could not archive the sheet.</span>}
      {clone.isError && <span data-testid="sheet-error" role="alert"> Could not clone the sheet.</span>}
    </form>
  );
}
