import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { peopleKey, peoplePath } from './people';
import type { Person } from './people';

type Imported = { added: number; updated: number; people: Person[] };

// The import box: lines of name,grade,terms,contact (the last two may be absent or blank), in order. A line whose name
// is a name or alias of a person on the sheet updates them as the person form would (terms and contact only when given);
// any other adds a person (a blank grade: the sheet's default grade). The server checks every line by the form's rules first (a person or contact repeated within
// the import too), so a refusal changes no one. Shows how many were added and how many updated.
function ImportPeople({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const importing = useMutation({
    mutationFn: (body: string) => postJson<Imported>(byUser(`${peoplePath(sheet)}/import`, user), { text: body }),
    onSuccess: (result) => {
      queryClient.setQueryData(peopleKey(sheet), result.people);
      setText('');
    },
  });
  const send = useSubmitOnce(importing);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send(text);
  };

  return (
    <form onSubmit={submit}>
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
            importing.reset();
          }}
        />
      </label>
      <br />
      <button type="submit" data-testid="import-submit" disabled={importing.isPending}>
        Import people
      </button>
      {importing.isSuccess && (
        <>
          {' '}Added <strong data-testid="import-added">{importing.data.added}</strong>, updated{' '}
          <strong data-testid="import-updated">{importing.data.updated}</strong>
        </>
      )}
      {importing.isError && (
        <span data-testid="import-error" role="alert">
          {' '}Nothing was imported: every line needs a name (a person on the sheet, by name or alias, or a new name not
          in use), naming no one twice, a grade of A, B or C (blank: the sheet's default grade), terms of whole days or blank, and a contact not another
          person's nor repeated.
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 11.5, Component: ImportPeople });
