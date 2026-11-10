import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { peopleKey } from '../people/people';

type Result = { added: number; updated: number };

// The import box: lines of name,grade,terms,contact (the terms and contact may be absent or blank)
// each add a person, in order, as the person form would; a line naming a person on the sheet (by name or
// alias) updates them instead, as the edit form would (terms and contact only when the line gives them). The server checks every line by the form's
// rules first (a name or contact repeated within the import is refused too): one bad line refuses the
// whole import and adds nothing. The box shows how many were added and updated; the counts is cleared while it runs.
function ImportPeople({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const importPeople = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/people/import`, body, user),
    onSuccess: (r) => {
      setResult(r);
      setText('');
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: () => setError('Those people could not be imported: check every line.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setResult(null);
    setError('');
    if (text.trim() === '') {
      setError('Paste at least one line of name,grade,terms,contact.');
      return;
    }
    once((done) => importPeople.mutate({ text }, { onSettled: done }));
  };

  return (
    <section data-testid="import-box">
      <form onSubmit={submit}>
        <label>
          Import people (name,grade,terms,contact){' '}
          <textarea
            data-testid="import-people"
            rows={4}
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="import-submit" disabled={importPeople.isPending}>
          Import people
        </button>
        {result !== null && (
          <>
            {' '}Added <span data-testid="import-added">{result.added}</span>, updated{' '}
            <span data-testid="import-updated">{result.updated}</span>
          </>
        )}
        {error && (
          <p role="alert" data-testid="import-error">
            {error}
          </p>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 1.4, Component: ImportPeople });
