import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson, putJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';

type SheetToday = { id: string; today: string; archived: boolean };

/** The key every feature reads the sheet's today under: date decisions and default dates use it, never the real clock. */
export const sheetTodayKey = (sheetId: string) => ['sheet', sheetId, 'today'] as const;

export function useSheetToday(sheetId: string) {
  return useQuery({
    queryKey: sheetTodayKey(sheetId),
    queryFn: () => getJson<SheetToday>(`/api/sheets/${encodeURIComponent(sheetId)}/today`),
  });
}

// The sheet's today: shown, and changeable. Changing it re-reads the whole sheet, since anything
// that depends on the date may now show differently. Also whether the sheet is archived, and the
// button a steward archives it with; the two share the sheet's error.
function SheetTodayPanel({ sheetId }: { sheetId: string; user: string }) {
  const today = useSheetToday(sheetId);
  const [input, setInput] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: async (date: string) => {
      if (date.trim() === '') throw new Error('Enter a date');
      return putJson<SheetToday>(`/api/sheets/${encodeURIComponent(sheetId)}/today`, { today: date.trim(), by: user ?? '' });
    },
    onSuccess: async (saved) => {
      queryClient.setQueryData(sheetTodayKey(sheetId), saved);
      await queryClient.invalidateQueries({
        queryKey: ['sheet', sheetId],
        predicate: (q) => q.queryKey[2] !== 'today',
      });
    },
  });

  const archive = useMutation({
    mutationFn: () => postJson<SheetToday>(`/api/sheets/${encodeURIComponent(sheetId)}/archive`, { by: user ?? '' }),
    onSuccess: (saved) => queryClient.setQueryData(sheetTodayKey(sheetId), saved),
  });

  const submit = useSubmitOnce(save);
  const submitArchive = useSubmitOnce(archive);
  return (
    <section data-testid="sheet-today-panel">
      <p>
        Today: <strong data-testid="sheet-today">{today.data?.today ?? ''}</strong>
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          archive.reset();
          submit(input);
        }}
      >
        <input
          type="date"
          data-testid="sheet-today-input"
          value={input}
          onChange={(e) => {
            setInput(e.target.value);
            save.reset();
            archive.reset();
          }}
        />{' '}
        <button type="submit" data-testid="sheet-today-save" disabled={save.isPending}>
          Set today
        </button>
      </form>
      <p>
        Archived:{' '}
        <strong data-testid="sheet-archived">{today.data === undefined ? '' : today.data.archived ? 'yes' : 'no'}</strong>{' '}
        <button
          type="button"
          data-testid="sheet-archive"
          disabled={archive.isPending}
          onClick={() => {
            save.reset();
            submitArchive(undefined);
          }}
        >
          Archive sheet
        </button>
      </p>
      {(save.isError || archive.isError || today.isError) && (
        <p data-testid="sheet-error" role="alert">
          {(save.error ?? archive.error ?? today.error)?.message}
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 0, Component: SheetTodayPanel });
