import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSheetToday, type SheetToday } from './useSheetToday';

/** The sheet's today, shown and changeable, and whether it is archived, with the steward's archive
 *  button and the steward's clone to another (never used) sheet id. Changing today re-reads the whole sheet,
 *  since every date decision in it depends on today. All share the one sheet error: the last of them refused. */
function TodayControl({ sheetId }: { sheetId: string }) {
  const today = useSheetToday(sheetId);
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (raw: string) =>
      postJson<SheetToday>(`/api/sheets/${encodeURIComponent(sheetId)}/today`, { date: raw.trim() }),
    onSuccess: (sheet) => {
      queryClient.setQueryData(['sheet', sheetId, 'today'], sheet);
      void queryClient.invalidateQueries({
        queryKey: ['sheet', sheetId],
        predicate: (q) => q.queryKey[2] !== 'today',
      });
    },
  });
  const archive = useMutation({
    mutationFn: () => postJson<SheetToday>(`/api/sheets/${encodeURIComponent(sheetId)}/archive`, {}),
    onSuccess: (sheet) => queryClient.setQueryData(['sheet', sheetId, 'today'], sheet),
  });
  const [cloneTo, setCloneTo] = useState('');
  const clone = useMutation({
    mutationFn: (toSheetId: string) =>
      postJson<{ clonedTo: string }>(`/api/sheets/${encodeURIComponent(sheetId)}/clone`, { toSheetId }),
  });
  const error = save.error ?? archive.error ?? clone.error;
  const resetAll = () => {
    save.reset();
    archive.reset();
    clone.reset();
  };

  return (
    <form
      data-testid="sheet-today-form"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          resetAll();
          return save.mutateAsync(date);
        });
      }}
    >
      Today: <strong data-testid="sheet-today">{today.data?.today ?? ''}</strong>{' '}
      <label>
        Set today{' '}
        <input
          type="text"
          placeholder="yyyy-mm-dd"
          data-testid="sheet-today-input"
          value={date}
          onChange={(e) => {
            setDate(e.target.value);
            resetAll();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
        Set
      </button>
      Archived: <strong data-testid="sheet-archived">{today.data ? (today.data.archived ? 'yes' : 'no') : ''}</strong>{' '}
      <button
        type="button"
        data-testid="sheet-archive"
        disabled={archive.isPending}
        onClick={() =>
          submitOnce(() => {
            resetAll();
            return archive.mutateAsync();
          })
        }
      >
        Archive sheet
      </button>{' '}
      <label>
        Clone to{' '}
        <input
          type="text"
          placeholder="Sheet id"
          data-testid="sheet-clone-to"
          value={cloneTo}
          onChange={(e) => {
            setCloneTo(e.target.value);
            resetAll();
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="sheet-clone"
        disabled={clone.isPending}
        onClick={() =>
          submitOnce(() => {
            resetAll();
            return clone.mutateAsync(cloneTo);
          })
        }
      >
        Clone sheet
      </button>{' '}
      Cloned to: <strong data-testid="sheet-cloned-to">{clone.data?.clonedTo ?? ''}</strong>
      {error && <p data-testid="sheet-error">{error.message}</p>}
    </form>
  );
}

export const contribution = SheetBody.fill({
  order: 0,
  Component: ({ sheetId }) => <TodayControl sheetId={sheetId} />,
});
