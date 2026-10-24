import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peoplePath, peopleQueryKey, type Person } from './people';

// A person's aliases — other names they go by, sorted ignoring case (the server sorts them) — and the entry and
// button that add another. An alias must not equal, ignoring case, any name or alias on the sheet.
function AliasesHeader() {
  return <th>Aliases</th>;
}

function PersonAliases({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [alias, setAlias] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { alias: string }) =>
      postJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/aliases`, body, user),
    onSuccess: () => {
      setAlias('');
      return queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
    },
  });
  const submit = useSubmitOnce(add);
  const rowAction = useRowAction(`person-${person.id}`);
  return (
    <td>
      <span data-testid="person-aliases">{(person.aliases ?? []).join(', ')}</span>{' '}
      <input
        data-testid="person-alias-input"
        placeholder="Alias"
        value={alias}
        onChange={(e) => { setAlias(e.target.value); add.reset(); }}
      />
      <button
        type="button"
        data-testid="person-alias-add"
        disabled={add.isPending}
        onClick={() => { rowAction.take(); submit({ alias }); }}
      >
        Add alias
      </button>
      {add.isError && rowAction.isLatest && (
        <span data-testid="person-error">The alias must not be blank, nor any name or alias on the sheet.</span>
      )}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 6.5, Component: AliasesHeader }),
  PersonCells.fill({ order: 6.5, Component: PersonAliases }),
];
