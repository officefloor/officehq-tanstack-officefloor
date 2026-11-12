import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
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
  const archive = useMutation({
    mutationFn: () => postJson<Sheet>(`/api/sheets/${encodeURIComponent(sheet)}/archive`, { user }),
    onSuccess: (archived) => queryClient.setQueryData(sheetKey(sheet), archived),
  });
  const submitArchive = useSubmitOnce(archive);
  // The sheet's one error element: a refused today or a refused archive, whichever was tried last.
  const failed = save.isError ? save.error : archive.isError ? archive.error : null;

  if (!user || !loaded.data) return null;
  return (
    <form
      data-testid="sheet-today-form"
      onSubmit={(e) => {
        e.preventDefault();
        archive.reset();
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
          archive.reset();
        }}
      />
      <button type="submit" data-testid="sheet-today-save">
        Set today
      </button>
      {' '}Archived <strong data-testid="sheet-archived">{loaded.data.archived ? 'yes' : 'no'}</strong>{' '}
      <button
        type="button"
        data-testid="sheet-archive"
        onClick={() => {
          save.reset();
          submitArchive();
        }}
      >
        Archive sheet
      </button>
      {failed && (
        <span data-testid="sheet-error" role="alert">
          {' '}{failed instanceof HttpError && failed.reason ? failed.reason : 'Not a valid date.'}
        </span>
      )}
    </form>
  );
}

export const contribution = AppHeader.fill({ order: 20, Component: SheetToday });
