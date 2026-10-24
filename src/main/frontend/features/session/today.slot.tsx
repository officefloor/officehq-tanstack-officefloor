import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { AppSession } from '../../slots/defs/appSession';
import { useCurrentSheet, useCurrentUser, useSheetInfo, sheetQueryKey, type SheetInfo } from './session';

// The sheet's today: shown and changeable here. Changing it re-reads the whole sheet, since every
// date decision in the app depends on it.
function SheetToday() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const info = useSheetInfo();
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (today: string) =>
      putJson<SheetInfo>(`/api/sheets/${encodeURIComponent(sheet ?? '')}/today`, { today }),
    onSuccess: async (saved) => {
      queryClient.setQueryData(sheetQueryKey(saved.id), saved);
      await queryClient.invalidateQueries({ queryKey: ['sheet', saved.id] });
    },
  });

  if (!user || !sheet) return null;
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save.mutate(date.trim());
      }}
    >
      <span>
        Today <strong data-testid="sheet-today">{info.data?.id === sheet ? info.data.today : ''}</strong>
      </span>
      <input
        data-testid="sheet-today-input"
        placeholder="YYYY-MM-DD"
        value={date}
        onChange={(e) => { setDate(e.target.value); save.reset(); }}
      />
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set today
      </button>
      {save.isError && <span data-testid="sheet-error">Could not change the sheet's today.</span>}
    </form>
  );
}

export const contribution = AppSession.fill({ order: 20, Component: SheetToday });
