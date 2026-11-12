import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { peopleKey } from './people';

type ImportResult = { added: number; updated: number };

// The import box: lines of name,grade,terms,contact each add a person, or update the person on the
// sheet the name (or alias) matches. The server checks every line with the person form's rules first
// and refuses the whole import if any fails; the counts added and updated show once it answers (gone
// while it runs).
function ImportPeople() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const queryClient = useQueryClient();

  const importing = useMutation({
    mutationFn: (t: string) =>
      postJson<ImportResult>(`/api/sheets/${encodeURIComponent(sheet)}/import-people`, { text: t, user }),
    // Not awaited: the count added shows as soon as the server answers, no later than the rows it added.
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });
  const submit = useSubmitOnce(importing);

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submit(text);
      }}
    >
      <h2>Import people</h2>
      <textarea
        data-testid="import-people"
        placeholder="name,grade,terms,contact — one person per line"
        rows={4}
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          importing.reset();
        }}
      />
      <button type="submit" data-testid="import-submit" disabled={importing.isPending}>
        Import people
      </button>
      {importing.isSuccess && (
        <>
          {' '}
          Added <strong data-testid="import-added">{importing.data.added}</strong>, updated{' '}
          <strong data-testid="import-updated">{importing.data.updated}</strong>
        </>
      )}
      {importing.isError && (
        <span data-testid="import-error" role="alert">
          {' '}
          {importing.error instanceof HttpError && importing.error.reason
            ? `${importing.error.reason}.`
            : 'The import was refused.'}
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 15.5, Component: ImportPeople });
