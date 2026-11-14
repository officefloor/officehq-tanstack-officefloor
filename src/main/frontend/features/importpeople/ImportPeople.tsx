import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';

type ImportResult = { added: number; updated: number };

// The import box: lines of name,grade,terms,contact (terms and contact may be left off or blank; a blank grade takes the sheet's default grade). A line naming a
// person on the sheet (by name or alias) updates them as the person form's edit does; any other adds a person.
// The server checks every line with the form's rules first, so one refused line writes nothing.
export function ImportPeople() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const [result, setResult] = useState<ImportResult | undefined>(undefined);
  const once = useOneAtATime();
  const queryClient = useQueryClient();

  const importPeople = useMutation({
    mutationFn: (pasted: string) =>
      postJson<ImportResult>(
        `/api/sheets/${encodeURIComponent(sheet)}/import-people?user=${encodeURIComponent(user)}`,
        { text: pasted },
      ),
    onSuccess: async (imported) => {
      setText('');
      // Re-read people first, then show them with the count in one render.
      const people = await getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`);
      queryClient.setQueryData(peopleKey(sheet), people);
      setResult(imported);
    },
    onError: () => setError('Nothing was imported: every line must be a person the person form takes.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setResult(undefined);
    if (!text.trim()) {
      setError('Paste at least one line of name,grade,terms,contact.');
      return;
    }
    once(() => importPeople.mutateAsync(text));
  };

  return (
    <form onSubmit={submit} data-testid="import-form">
      <textarea
        data-testid="import-people"
        placeholder="name,grade,terms,contact — one person per line"
        rows={4}
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          setError('');
        }}
      />
      <button type="submit" data-testid="import-submit" disabled={importPeople.isPending}>
        Import people
      </button>
      {result !== undefined && (
        <>
          {' '}
          Added <span data-testid="import-added">{result.added}</span>, updated{' '}
          <span data-testid="import-updated">{result.updated}</span>
        </>
      )}
      {error && <span data-testid="import-error">{error}</span>}
    </form>
  );
}
