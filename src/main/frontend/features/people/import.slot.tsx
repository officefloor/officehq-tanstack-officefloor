import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { claimsKey } from '../claims/claims';
import { peopleKey } from './people';

type ImportResult = { added: number; updated: number };

/** The import box: one name,grade,terms,contact line per person (the terms and contact may be absent or blank; a blank grade is the sheet's default grade). The
 *  server checks every line by the person form's rules first (and no name or contact repeated within the paste); one
 *  failing line refuses the whole import and shows why on the box, otherwise each line in order updates the person on
 *  the sheet with that name or alias as the form's edit would, or else adds a person as the form would, and the box
 *  shows how many were added and how many updated. */
function ImportPeople({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [result, setResult] = useState<ImportResult | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const importPeople = useMutation({
    mutationFn: (body: { user: string; text: string }) =>
      postJson<ImportResult>(`/api/sheets/${encodeURIComponent(sheetId)}/import-people`, body),
    onSuccess: async (r) => {
      setResult(r);
      setText('');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
    onError: (e) =>
      setError(
        e instanceof HttpError && e.body
          ? e.body
          : 'Could not import: each line is name,grade,terms,contact.',
      ),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setResult(null);
    if (text.trim() === '') {
      setError('Paste at least one line: name,grade,terms,contact.');
      return;
    }
    if (!once.start()) return;
    importPeople.mutate({ user, text });
  };

  return (
    <form data-testid="import-form" onSubmit={submit}>
      <label>
        Import people (name,grade,terms,contact per line){' '}
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
        <p data-testid="import-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: 11.2, Component: ImportPeople });
