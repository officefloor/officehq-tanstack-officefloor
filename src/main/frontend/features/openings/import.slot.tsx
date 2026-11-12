import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { openingsKey } from './openings';

type ImportResult = { set: number };

// The opening balance box: lines of name,period,amount each add the amount to the person's (by name or alias)
// carried figure for the period. The server checks every line first and refuses the whole paste if any
// fails; the count of lines set shows once it answers (gone while it runs).
function ImportOpenings() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const queryClient = useQueryClient();

  const importOpenings = useMutation({
    mutationFn: (t: string) =>
      postJson<ImportResult>(`/api/sheets/${encodeURIComponent(sheet)}/import-openings`, { text: t, user }),
    // Not awaited: the count set shows as soon as the server answers.
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: openingsKey(sheet) });
    },
  });
  const submit = useSubmitOnce(importOpenings);

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submit(text);
      }}
    >
      <h2>Opening balances</h2>
      <textarea
        data-testid="opening-import"
        placeholder="name,period,amount — one opening balance per line"
        rows={4}
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          importOpenings.reset();
        }}
      />
      <button type="submit" data-testid="opening-import-submit" disabled={importOpenings.isPending}>
        Paste openings
      </button>
      {importOpenings.isSuccess && (
        <>
          {' '}
          Set <strong data-testid="opening-import-done">{importOpenings.data.set}</strong>
        </>
      )}
      {importOpenings.isError && (
        <span data-testid="opening-error" role="alert">
          {' '}
          {importOpenings.error instanceof HttpError && importOpenings.error.reason
            ? `${importOpenings.error.reason}.`
            : 'The paste was refused.'}
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 42, Component: ImportOpenings });
