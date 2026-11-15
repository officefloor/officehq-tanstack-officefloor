import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { importPeople, peopleKey } from './api';

/** A box that adds a person per pasted line of name,grade,terms,contact, or updates the person the
 *  line's name (or alias) matches, and how many it added and updated.
 *  The count is cleared while it runs; a refused import adds nothing and shows an error. */
export function ImportPeople({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const submitOnce = useSubmitOnce();

  const imported = useMutation({
    mutationFn: (input: string) => importPeople(sheetId, user, input),
    // Not awaited: the count shows as soon as the import is done, never after its new rows.
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }); },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const input = text;
        submitOnce(() => {
          imported.reset();
          return imported.mutateAsync(input);
        });
      }}
    >
      <label>
        Import people (name,grade,terms,contact per line; terms and contact may be left out; a blank grade takes the default grade){' '}
        <textarea data-testid="import-people" rows={3} value={text}
          onChange={(e) => {
            setText(e.target.value);
            if (imported.isError) imported.reset();
          }} />
      </label>{' '}
      <button type="submit" data-testid="import-submit" disabled={imported.isPending}>
        Import people
      </button>
      {imported.isSuccess && (
        <>
          {' '}Added <span data-testid="import-added">{imported.data.added}</span>
          {', '}updated <span data-testid="import-updated">{imported.data.updated}</span>
        </>
      )}
      {imported.isError && (
        <span data-testid="import-error">Every line needs a name not repeated (a name or alias on the sheet updates that person, once), a contact (if any) not used by anyone else nor repeated, a grade of A, B or C, and terms (if any) a whole number of days: nothing was imported.</span>
      )}
    </form>
  );
}
