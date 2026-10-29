import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleKey } from './peopleQuery';
import { useUser } from '../session/useUser';

/**
 * The import box: lines of name,grade,terms,contact, in order; a blank grade is the sheet's default grade. A line naming a person on the sheet (by name or alias)
 * updates them as their edit form would; any other line adds a person as the person form adds one. The server checks
 * every line by the form's rules first (a name or contact repeated in the import is refused too) and stores nothing
 * when any is refused. Shows how many were added and how many updated.
 */
function ImportPeople({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const [added, setAdded] = useState('');
  const [updated, setUpdated] = useState('');
  const once = useSubmitOnce();
  const importPeople = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<{ added: number; updated: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/people/import`, body, user),
    onSuccess: async (res) => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      setText('');
      setAdded(String(res.added));
      setUpdated(String(res.updated));
    },
    onError: (e) =>
      setError(e instanceof RefusedError ? e.message : 'The people could not be imported: check every line.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setAdded('');
    setUpdated('');
    if (text.trim() === '') {
      setError('Paste at least one line of name,grade,terms,contact.');
      return;
    }
    once(() => importPeople.mutateAsync({ text }));
  };

  return (
    <form data-testid="import-box" onSubmit={submit}>
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
        Import
      </button>{' '}
      Added: <span data-testid="import-added">{added}</span>{' '}
      Updated: <span data-testid="import-updated">{updated}</span>
      {error && <p data-testid="import-error">{error}</p>}
    </form>
  );
}

// Below the person form; keyed by sheet, so pasted text, a count or an error never carries over.
export const contribution = SheetPanel.fill({
  order: 10.5,
  Component: ({ sheetId }) => <ImportPeople key={sheetId} sheetId={sheetId} />,
});
