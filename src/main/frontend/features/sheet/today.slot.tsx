import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/usePeople';
import { periodsKey } from '../periods/usePeriods';
import { settingsKey, useSettings } from '../settings/useSettings';
import { sheetKey, useSheet, type Sheet } from './useSheet';

/** Shows the sheet's today and changes it; shows its parent sheet (nothing when none); shows whether the sheet is archived and archives it (a
 *  steward only); clones the sheet to another, never used, id (a steward only) and shows the last clone made. All share the sheet's error. */
function SheetToday({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data } = useSheet(sheet);
  const { data: settings } = useSettings(sheet);
  const [date, setDate] = useState('');
  const [cloneTo, setCloneTo] = useState('');
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
        // A today past a period's grace days closes it.
        queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
        // A moved today keeps a snapshot line on each person.
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });
  const archive = useMutation({
    mutationFn: () => postJson<Sheet>(`/api/sheets/${encodeURIComponent(sheet)}/archive`, { user }),
    onSuccess: (saved) => {
      queryClient.setQueryData(sheetKey(sheet), saved);
    },
  });
  const clone = useMutation({
    mutationFn: (to: string) => postJson<Sheet>(`/api/sheets/${encodeURIComponent(sheet)}/clone`, { to, user }),
  });

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
            archive.reset();
            clone.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Save
      </button>
      {' '}Parent: <strong data-testid="sheet-parent">{settings?.parent ?? ''}</strong>{' '}
      {' '}Archived: <strong data-testid="sheet-archived">{data ? (data.archived ? 'yes' : 'no') : ''}</strong>{' '}
      <button
        type="button"
        data-testid="sheet-archive"
        disabled={archive.isPending}
        onClick={() => {
          save.reset();
          archive.reset();
          clone.reset();
          submitOnce(() => archive.mutateAsync());
        }}
      >
        Archive
      </button>{' '}
      <label>
        Clone to{' '}
        <input
          data-testid="sheet-clone-to"
          placeholder="Sheet id"
          value={cloneTo}
          onChange={(e) => {
            setCloneTo(e.target.value);
            save.reset();
            archive.reset();
            clone.reset();
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="sheet-clone"
        disabled={clone.isPending}
        onClick={() => {
          save.reset();
          archive.reset();
          clone.reset();
          submitOnce(() => clone.mutateAsync(cloneTo.trim()));
        }}
      >
        Clone
      </button>
      {' '}Cloned to: <strong data-testid="sheet-cloned-to">{clone.data?.id ?? ''}</strong>
      {(save.isError || archive.isError || clone.isError) && (
        <span data-testid="sheet-error" role="alert">
          {' '}
          {clone.isError
            ? 'Could not clone: only a steward can clone the sheet, to an id never used.'
            : archive.isError
            ? data?.archived
              ? 'Could not archive: the sheet is already archived.'
              : 'Could not archive: only a steward can archive the sheet.'
            : data?.archived
              ? 'Could not set today: the sheet is archived.'
              : 'Could not set today: enter a date as yyyy-mm-dd.'}
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 0, Component: SheetToday });
