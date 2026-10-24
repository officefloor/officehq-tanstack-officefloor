import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peopleQueryKey } from './people';

// The people import box: each line is name,grade,terms,contact (the terms and the contact may be blank or absent)
// and adds a person, in order, as the person form would. The server checks every line by the person form's rules
// first — a name (ignoring case) or a contact repeated in the paste is refused too — so one refused line adds
// nothing. How many were added shows once done, and is gone while it runs.
function ImportPeople() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const importPeople = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<{ added: number }>(`/api/sheets/${encodeURIComponent(sheet ?? '')}/import-people`, body, user),
    // Not awaited: the count shows before the new rows do.
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
    },
  });
  const submit = useSubmitOnce(importPeople);
  if (!user || !sheet) return null;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submit({ text });
      }}
    >
      <h2>Import people</h2>
      <textarea
        data-testid="import-people"
        placeholder="name,grade,terms,contact — one person per line"
        rows={4}
        value={text}
        onChange={(e) => { setText(e.target.value); importPeople.reset(); }}
      />
      <button type="submit" data-testid="import-submit" disabled={importPeople.isPending}>
        Import people
      </button>
      {importPeople.isSuccess && (
        <>
          {' '}Added <span data-testid="import-added">{importPeople.data.added}</span>
        </>
      )}
      {importPeople.isError && (
        <span data-testid="import-error">
          Nothing was imported: every line needs a name and a grade of A, B or C; terms must be a whole number of
          days; no name may be in use on the sheet or its tree, nor a contact on the sheet, and none may repeat in
          the paste.
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 15.5, Component: ImportPeople });
