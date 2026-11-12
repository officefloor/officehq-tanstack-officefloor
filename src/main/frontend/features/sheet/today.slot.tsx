import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { AppHeader } from '../../slots/defs/appHeader';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Sheet, sheetKey, useSheet } from './sheet';

function SheetToday() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const loaded = useSheet(sheet);
  const [input, setInput] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (today: string) =>
      postJson<Sheet>(`/api/sheets/${encodeURIComponent(sheet)}/today`, { today, user }),
    // A new today changes every date decision on the sheet: re-read all of it.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: sheetKey(sheet) }),
  });
  const submit = useSubmitOnce(save);

  if (!user || !loaded.data) return null;
  return (
    <form
      data-testid="sheet-today-form"
      onSubmit={(e) => {
        e.preventDefault();
        submit(input.trim());
      }}
    >
      Today <strong data-testid="sheet-today">{loaded.data.today}</strong>{' '}
      <input
        data-testid="sheet-today-input"
        type="date"
        value={input}
        onChange={(e) => {
          setInput(e.target.value);
          save.reset();
        }}
      />
      <button type="submit" data-testid="sheet-today-save">
        Set today
      </button>
      {save.isError && (
        <span data-testid="sheet-error" role="alert">
          {' '}Not a valid date.
        </span>
      )}
    </form>
  );
}

export const contribution = AppHeader.fill({ order: 20, Component: SheetToday });
