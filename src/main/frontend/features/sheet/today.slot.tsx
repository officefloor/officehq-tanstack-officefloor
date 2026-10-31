import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { settingsKey } from '../settings/useSettings';
import { sheetKey, useSheet, type Sheet } from './useSheet';

/** Shows the sheet's today and changes it. */
function SheetToday({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data } = useSheet(sheet);
  const [date, setDate] = useState('');
  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (today: string) =>
      postJson<Sheet>(`/api/sheets/${encodeURIComponent(sheet)}/today`, { today, user }),
    onSuccess: (saved) => {
      queryClient.setQueryData(sheetKey(sheet), saved);
      // A blank policy date is the sheet's today: it decides how each claim's live figures round.
      // The dated settings show (and the clerk limit and cap apply) as in force on the sheet's today.
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: settingsKey(sheet) }),
      ]);
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
      Today: <strong data-testid="sheet-today">{data?.today ?? ''}</strong>{' '}
      <label>
        Set today{' '}
        <input
          data-testid="sheet-today-input"
          placeholder="yyyy-mm-dd"
          value={date}
          onChange={(e) => {
            setDate(e.target.value);
            save.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Save
      </button>
      {save.isError && (
        <span data-testid="sheet-error" role="alert">
          {' '}Could not set today: enter a date as yyyy-mm-dd.
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 0, Component: SheetToday });
