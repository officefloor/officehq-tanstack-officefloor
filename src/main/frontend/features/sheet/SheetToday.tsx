import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError } from '../../api/http';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { listSettings, settingOn, settingsKey } from '../settings/api';
import { archiveSheet, cloneSheet, setSheetToday, sheetKey, type Sheet } from './api';

/** Whether a change was refused rather than invalid (the server answers 403: a viewer, an archived
 *  sheet, or archiving by anyone but a steward). */
function refused(error: Error | null): boolean {
  return error instanceof HttpError && error.status === 403;
}

/** The sheet's today, shown and changeable, and whether it is archived, with the button that archives
 *  it, and the entry and button that clone it to another id (the last clone made is shown). Every date
 *  on the sheet goes by its today. They share the sheet's one error element. */
export function SheetToday({ sheet, user }: { sheet: Sheet; user: string }) {
  const queryClient = useQueryClient();
  const [date, setDate] = useState('');
  const settings = useQuery({ queryKey: settingsKey(sheet.id), queryFn: () => listSettings(sheet.id) });
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

  const archive = useMutation({
    mutationFn: () => archiveSheet(sheet.id, user),
    onSuccess: (updated) => queryClient.setQueryData(sheetKey(sheet.id), updated),
  });

  const [cloneTo, setCloneTo] = useState('');
  const clone = useMutation({ mutationFn: (to: string) => cloneSheet(sheet.id, user, to) });

  const submitOnce = useSubmitOnce();

  const resetAll = () => {
    save.reset();
    archive.reset();
    clone.reset();
  };

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const today = date.trim();
        submitOnce(() => {
          resetAll();
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
          resetAll();
        }}
      />{' '}
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set today
      </button>
      Parent <span data-testid="sheet-parent">{settingOn(settings.data, 'parent', sheet.today) ?? ''}</span>{' '}
      Archived <span data-testid="sheet-archived">{sheet.archived ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="sheet-archive"
        disabled={archive.isPending}
        onClick={() => {
          resetAll();
          archive.mutate();
        }}
      >
        Archive
      </button>{' '}
      <input
        data-testid="sheet-clone-to"
        placeholder="Clone to sheet id"
        value={cloneTo}
        onChange={(e) => {
          setCloneTo(e.target.value);
          resetAll();
        }}
      />{' '}
      <button
        type="button"
        data-testid="sheet-clone"
        disabled={clone.isPending}
        onClick={() => {
          resetAll();
          clone.mutate(cloneTo.trim());
        }}
      >
        Clone
      </button>{' '}
      Cloned to <span data-testid="sheet-cloned-to">{clone.data?.to ?? ''}</span>{' '}
      {save.isError && (
        <span data-testid="sheet-error">
          {refused(save.error) ? 'The sheet cannot be changed.' : 'That is not a date.'}
        </span>
      )}
      {archive.isError && (
        <span data-testid="sheet-error">
          Only a steward may archive the sheet, once.
        </span>
      )}
      {clone.isError && (
        <span data-testid="sheet-error">
          Only a steward may clone the sheet, and only to a sheet never used.
        </span>
      )}
    </form>
  );
}
