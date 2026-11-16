import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { claimsKey } from '../claims/claims';
import { peopleKey } from '../people/people';

type Result = { added: number };

/** The import box: lines of name,grade,terms,contact (terms and contact may be absent or blank) each add a person, in
 *  order, as the person form adds one. The server checks every line by the person form's rules first (a name or
 *  contact repeated within the paste counts as taken): one failing line refuses the import and adds nothing. Shows how
 *  many were added; the count is cleared while a new press runs. */
function ImportPeople({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const submit = useSubmitOnce();
  const importPeople = useMutation({
    mutationFn: (t: string) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/import-people`, { text: t, by: user }),
    onSuccess: () => {
      setText('');
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!submit.begin()) return;
        importPeople.reset();
        importPeople.mutate(text);
      }}
    >
      <label>
        Import people (name,grade,terms,contact per line)
        <br />
        <textarea
          data-testid="import-people"
          rows={4}
          cols={50}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            importPeople.reset();
          }}
        />
      </label>
      <br />
      <button type="submit" data-testid="import-submit" disabled={importPeople.isPending}>
        Import people
      </button>
      {importPeople.isSuccess && (
        <>
          {' '}
          Added <span data-testid="import-added">{importPeople.data.added}</span>
        </>
      )}
      {importPeople.isError && (
        <span data-testid="import-error">
          {' '}
          Nothing was imported: every line must keep the person form's rules, with no name or contact already on the
          sheet or repeated in the paste.
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPage.fill({ order: 12, Component: ImportPeople });
