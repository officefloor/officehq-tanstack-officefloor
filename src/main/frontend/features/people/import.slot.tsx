import { useState } from 'react';
import type { FormEvent } from 'react';
import { HttpError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { importPeople, peopleKey } from './people';
import type { ImportResult } from './people';

// Import people pasted as lines of name,grade. The server checks every line first; when any is bad
// nothing is imported and each bad line is marked by its number, otherwise the counts added and
// updated (a name already on the sheet updates that person's grade) are shown.
function ImportPeople() {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const [text, setText] = useState('');
  const [result, setResult] = useState<ImportResult | null>(null);
  const [failed, setFailed] = useState(false);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const run = useMutation({
    mutationFn: (input: string) => importPeople(sheet!, user, input),
    onSuccess: async (r) => {
      setResult(r);
      setText('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet!) });
    },
    onError: (e) => {
      const body = e instanceof HttpError ? (e.body as ImportResult | undefined) : undefined;
      if (body && Array.isArray(body.errors)) setResult({ added: 0, updated: 0, errors: body.errors });
      else setFailed(true);
    },
  });

  if (!sheet) return null;

  const clear = () => {
    setResult(null);
    setFailed(false);
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    clear();
    once(() => run.mutateAsync(text));
  };

  return (
    <section data-testid="import">
      <h2>Import people</h2>
      <form data-testid="import-form" onSubmit={submit}>
        <textarea
          data-testid="import-text"
          placeholder="name,grade — one person per line"
          rows={4}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            clear();
          }}
        />
        <button type="submit" data-testid="import-submit" disabled={run.isPending}>
          Import
        </button>
      </form>
      {result && (
        <p>
          Added <span data-testid="import-added">{result.added}</span>, updated{' '}
          <span data-testid="import-updated">{result.updated}</span>
          {result.errors.length > 0 && <> — nothing was imported; fix these lines:</>}
        </p>
      )}
      {result && result.errors.length > 0 && (
        <ul>
          {result.errors.map((n) => (
            <li key={n} data-testid={`import-error-${n}`}>
              Line {n}: a name is required, not repeated in the paste, with a grade of A, B or C.
            </li>
          ))}
        </ul>
      )}
      {failed && <span data-testid="import-failed">The import could not be made.</span>}
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 15, Component: ImportPeople });
