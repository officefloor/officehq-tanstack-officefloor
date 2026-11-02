import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useIsLatest } from '../../query/useIsLatest';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { useShown } from '../columns/columns';
import { peoplePath, personActionKey } from '../people/people';
import type { Person } from '../people/people';
import { aliasesKey, byNameKey, useAliases } from './aliases';
import type { Alias } from './aliases';

function AliasesHeader({ sheet, user }: { sheet: string; user: string }) {
  const shown = useShown(sheet, user);
  return (
    <>
      {shown('aliases') && <th>Aliases</th>}
      <th />
    </>
  );
}

// A person's aliases (sorted as names are compared, comma separated) and the entry that adds one. An alias shares
// the names' uniqueness: the server refuses a blank one, or one equal to any name or alias on the sheet.
function PersonAliases({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const shown = useShown(sheet, user);
  const queryClient = useQueryClient();
  const { data: aliases = [] } = useAliases(sheet);
  const [typed, setTyped] = useState('');

  const mine = aliases
    .filter((a) => a.person === person.id)
    .map((a) => a.alias)
    .sort(byNameKey);

  const add = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (alias: string) =>
      postJson<Alias[]>(byUser(`${peoplePath(sheet)}/${person.id}/aliases`, user), { alias }),
    onSuccess: (updated) => {
      queryClient.setQueryData(aliasesKey(sheet), updated);
      setTyped('');
    },
  });
  const send = useSubmitOnce(add);
  const latest = useIsLatest(personActionKey(sheet, person.id), add.submittedAt);

  return (
    <>
      {shown('aliases') && <td data-testid="person-aliases">{mine.join(', ')}</td>}
      <td>
        <input
          data-testid="person-alias-input"
          value={typed}
          placeholder="Alias"
          onChange={(e) => {
            setTyped(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="button" data-testid="person-alias-add" disabled={add.isPending} onClick={() => send(typed)}>
          Add alias
        </button>
        {add.isError && latest && (
          <span data-testid="person-error" role="alert">
            {' '}An alias must not be blank, nor any name or alias on the sheet.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 3, Component: AliasesHeader }),
  PersonColumns.fill({ order: 3, Component: PersonAliases }),
];
