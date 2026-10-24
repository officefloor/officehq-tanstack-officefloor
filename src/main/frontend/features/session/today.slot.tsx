import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { AppSession } from '../../slots/defs/appSession';
import { useCurrentSheet, useCurrentUser, useSheetInfo, sheetQueryKey, type SheetInfo } from './session';
import { useSettings } from '../roles/roles';

// The sheet's today: shown and changeable here, with the parent sheet it names (blank when none). Changing it re-reads the whole sheet, since every
// date decision in the app depends on it. The sheet is archived here too (a steward only), and cloned to another id
// never used (a steward only; the id of the last clone made is shown); the three share the sheet's one error element,
// which any of them clears.
function SheetToday() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const info = useSheetInfo();
  const settings = useSettings();
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (today: string) =>
      putJson<SheetInfo>(`/api/sheets/${encodeURIComponent(sheet ?? '')}/today`, { today }, user),
    onSuccess: async (saved) => {
      queryClient.setQueryData(sheetQueryKey(saved.id), saved);
      await queryClient.invalidateQueries({ queryKey: ['sheet', saved.id] });
    },
  });
  const submit = useSubmitOnce(save);
  const archive = useMutation({
    mutationFn: () => putJson<SheetInfo>(`/api/sheets/${encodeURIComponent(sheet ?? '')}/archive`, undefined, user),
    onSuccess: (saved) => queryClient.setQueryData(sheetQueryKey(saved.id), saved),
  });
  const submitArchive = useSubmitOnce(archive);
  const [cloneTo, setCloneTo] = useState('');
  const clone = useMutation({
    mutationFn: (toSheet: string) =>
      putJson<{ toSheet: string }>(`/api/sheets/${encodeURIComponent(sheet ?? '')}/clone`, { toSheet }, user),
  });
  const submitClone = useSubmitOnce(clone);
  const shown = info.data?.id === sheet ? info.data : undefined;

  if (!user || !sheet) return null;
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        archive.reset();
        clone.reset();
        submit(date.trim());
      }}
    >
      <span>
        Today <strong data-testid="sheet-today">{shown ? shown.today : ''}</strong>
      </span>
      <input
        data-testid="sheet-today-input"
        placeholder="YYYY-MM-DD"
        value={date}
        onChange={(e) => { setDate(e.target.value); save.reset(); archive.reset(); clone.reset(); }}
      />
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set today
      </button>
      <span>
        Archived <strong data-testid="sheet-archived">{shown ? (shown.archived ? 'yes' : 'no') : ''}</strong>
      </span>
      <span>
        Parent <strong data-testid="sheet-parent">{shown ? (settings.data?.parent ?? '') : ''}</strong>
      </span>
      <button
        type="button"
        data-testid="sheet-archive"
        disabled={archive.isPending}
        onClick={() => { save.reset(); clone.reset(); submitArchive(undefined); }}
      >
        Archive sheet
      </button>
      <input
        data-testid="sheet-clone-to"
        placeholder="Clone to sheet id"
        value={cloneTo}
        onChange={(e) => { setCloneTo(e.target.value); save.reset(); archive.reset(); clone.reset(); }}
      />
      <button
        type="button"
        data-testid="sheet-clone"
        disabled={clone.isPending}
        onClick={() => { save.reset(); archive.reset(); submitClone(cloneTo.trim()); }}
      >
        Clone sheet
      </button>
      <span>
        Cloned to <strong data-testid="sheet-cloned-to">{clone.isSuccess ? clone.data.toSheet : ''}</strong>
      </span>
      {save.isError && <span data-testid="sheet-error">Could not change the sheet's today.</span>}
      {archive.isError && <span data-testid="sheet-error">Could not archive the sheet.</span>}
      {clone.isError && <span data-testid="sheet-error">Could not clone the sheet.</span>}
    </form>
  );
}

export const contribution = AppSession.fill({ order: 20, Component: SheetToday });
