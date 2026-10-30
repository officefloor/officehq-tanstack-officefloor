import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { peopleKey } from './usePeople';

// The import box: one person per line as name,grade,terms,contact (terms and contact may be absent or
// blank). Every line is checked first by the person form's rules, and a name or contact given twice in
// the import refuses it; one refused line adds nothing. Otherwise each person is added in order as the
// form adds them, then shows how many were added; pressing again removes the old count until the new
// one is in.
export function ImportPeople() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();

  const importPeople = useMutation({
    mutationFn: (pasted: string) =>
      postJson<{ added: number }>(`/api/sheets/${encodeURIComponent(sheet)}/import-people`, { text: pasted, user }),
    onSuccess: () => {
      setText('');
      // Not awaited: the count shows as soon as the import is in, never after the rows it added.
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          importPeople.reset();
          return importPeople.mutateAsync(text);
        });
      }}
    >
      <textarea
        data-testid="import-people"
        value={text}
        rows={3}
        placeholder="name,grade,terms,contact — one person per line"
        onChange={(e) => {
          setText(e.target.value);
          importPeople.reset();
        }}
      />{' '}
      <button type="submit" data-testid="import-submit">Import people</button>
      {importPeople.isSuccess && (
        <>
          {' '}
          Added <span data-testid="import-added">{importPeople.data.added}</span>
        </>
      )}
      {importPeople.isError && (
        <span data-testid="import-error" role="alert">
          {' '}
          Nothing was imported: every line needs a name used by no one on the sheet (nor any alias) and a grade of A, B
          or C, terms of whole days, and a contact used by no one; no name or contact may be given twice in the import.
        </span>
      )}
    </form>
  );
}
