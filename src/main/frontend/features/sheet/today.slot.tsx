import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSheet, type Sheet } from '../session/session';

/** The sheet's today: shown, and changeable. Changing it re-reads the whole sheet. */
function SheetToday({ sheetId, user }: { sheetId: string; user: string }) {
  const sheet = useSheet(sheetId);
  const queryClient = useQueryClient();
  const [typed, setTyped] = useState('');
  const save = useMutation({
    mutationFn: (today: string) =>
      putJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}/today`, { today, by: user }),
    onSuccess: (saved) => {
      queryClient.setQueryData(['sheet', sheetId], saved);
      void queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
  });
  return (
    <form
      data-testid="sheet-today-form"
      onSubmit={(e) => {
        e.preventDefault();
        save.mutate(typed.trim());
      }}
    >
      Today <span data-testid="sheet-today">{sheet.data?.today}</span>{' '}
      <input
        type="date"
        data-testid="sheet-today-input"
        value={typed}
        onChange={(e) => {
          setTyped(e.target.value);
          save.reset();
        }}
      />
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set today
      </button>
      {save.isError && <span data-testid="sheet-error">Could not change today.</span>}
    </form>
  );
}

export const contribution = SheetPage.fill({ order: 0, Component: SheetToday });
