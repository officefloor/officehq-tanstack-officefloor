import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson, putJson } from '../../api/http';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/people.slot';
import { periodsKey } from '../periods/periods.slot';
import { notesKey } from '../notes/notes.slot';
import { useSheetId } from '../session/useSession';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';

// Choose a sheet by typing its id and loading it. Loading drops everything cached for that sheet,
// so the screen re-reads it from the store (an id never used before starts empty). Its people, claims
// periods and notes are read before it becomes the active sheet, so its rows show as soon as its id does.
export function SheetPicker() {
  const [sheetId, setSheetId] = useSheetId();
  const [input, setInput] = useState('');
  const queryClient = useQueryClient();
  const load = useMutation({
    mutationFn: async (id: string) => {
      if (id.trim() === '') throw new Error('Enter a sheet id');
      return putJson<{ id: string }>(`/api/sheets/${encodeURIComponent(id.trim())}`);
    },
    onSuccess: async (sheet) => {
      queryClient.removeQueries({ queryKey: ['sheet', sheet.id] });
      const base = `/api/sheets/${encodeURIComponent(sheet.id)}`;
      await Promise.all([
        queryClient.fetchQuery({ queryKey: peopleKey(sheet.id), queryFn: () => getJson(`${base}/people`) }),
        queryClient.fetchQuery({ queryKey: claimsKey(sheet.id), queryFn: () => getJson(`${base}/claims`) }),
        queryClient.fetchQuery({ queryKey: periodsKey(sheet.id), queryFn: () => getJson(`${base}/periods`) }),
        queryClient.fetchQuery({ queryKey: notesKey(sheet.id), queryFn: () => getJson(`${base}/notes`) }),
      ]).catch(() => {
        // the panels show their own read errors
      });
      setSheetId(sheet.id);
    },
  });

  const submit = useSubmitOnce(load);
  return (
    <div data-testid="sheet-bar">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit(input);
        }}
      >
        <label>
          Sheet{' '}
          <input
            data-testid="sheet-id-input"
            value={input}
            onChange={(e) => {
              setInput(e.target.value);
              load.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="sheet-load" disabled={load.isPending}>
          Load
        </button>
      </form>
      <p>
        Active sheet: <strong data-testid="sheet-current-id">{sheetId}</strong>
      </p>
      {load.isError && (
        <p data-testid="sheet-load-error" role="alert">
          {load.error.message}
        </p>
      )}
    </div>
  );
}
