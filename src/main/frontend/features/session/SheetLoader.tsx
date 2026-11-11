import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { useSheetId, useUser } from './session';
import { peopleQuery } from '../people/people';
import { claimsQuery } from '../claims/claims';
import { notesQuery } from '../notes/notes';
import { myRoleQuery } from '../dashboard/dashboard';
import { crewsQuery } from '../crews/crews';
import { ratesQuery } from '../rates/rates';
import { settingsQuery } from '../access/access';

type Sheet = { id: string };

// Choose a sheet by typing its id and loading it. An id never used before starts an empty sheet;
// loading an id again drops what was cached for it, so the sheet is re-read from the store. The
// sheet becomes the active one only once its people, claims and notes are read, and the user's role
// and the crews that decide which rows they see, and the rates and settings its figures are worked by, so its rows are on screen as soon as its id is.
export function SheetLoader({ onLoaded }: { onLoaded: () => void }) {
  const queryClient = useQueryClient();
  const [sheetId, setSheetId] = useSheetId();
  const [user] = useUser();
  const [input, setInput] = useState('');
  const [error, setError] = useState('');

  const once = useOnce();
  const load = useMutation({
    mutationFn: async (id: string) => {
      const sheet = await postJson<Sheet>(`/api/sheets/${encodeURIComponent(id)}/load`, {});
      queryClient.removeQueries({ queryKey: ['sheet', sheet.id] });
      await Promise.all([
        queryClient.fetchQuery(peopleQuery(sheet.id)),
        queryClient.fetchQuery(claimsQuery(sheet.id)),
        queryClient.fetchQuery(notesQuery(sheet.id)),
        queryClient.fetchQuery(myRoleQuery(sheet.id, user)),
        queryClient.fetchQuery(crewsQuery(sheet.id)),
        queryClient.fetchQuery(ratesQuery(sheet.id)),
        queryClient.fetchQuery(settingsQuery(sheet.id)),
      ]);
      return sheet;
    },
    onSuccess: (sheet) => {
      setSheetId(sheet.id);
      onLoaded();
    },
    onError: () => setError('That sheet could not be loaded.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const id = input.trim();
    if (id === '') {
      setError('Type a sheet id to load.');
      return;
    }
    once((done) => load.mutate(id, { onSettled: done }));
  };

  return (
    <form onSubmit={submit} data-testid="sheet-form">
      <label>
        Sheet{' '}
        <input
          data-testid="sheet-id-input"
          value={input}
          onChange={(e) => {
            setInput(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="sheet-load" disabled={load.isPending}>
        Load
      </button>{' '}
      {sheetId && (
        <span>
          Active sheet: <strong data-testid="sheet-current-id">{sheetId}</strong>
        </span>
      )}
      {error && (
        <p role="alert" data-testid="sheet-load-error">
          {error}
        </p>
      )}
    </form>
  );
}
