import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { peopleKey, type Person } from '../people/people';
import { usePersonRowAction } from '../people/rowAction';

function AliasesHead() {
  return (
    <>
      <th>Aliases</th>
      <th />
    </>
  );
}

/** A person's aliases (sorted as names are compared, by the server) and the entry that gives them another. An alias
 *  shares the names' uniqueness: one blank, or equal to any name or alias on the sheet, is refused with the row's error. */
function PersonAliases({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [alias, setAlias] = useState('');
  const submit = useSubmitOnce();
  const rowAction = usePersonRowAction(sheetId, person.id, 'alias');
  const add = useMutation({
    mutationFn: (value: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/aliases`, { alias: value, by: user }),
    onSuccess: () => {
      setAlias('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <>
      <td data-testid="person-aliases">{(person.aliases ?? []).join(', ')}</td>
      <td>
        <input
          data-testid="person-alias-input"
          placeholder="Alias"
          value={alias}
          onChange={(e) => {
            setAlias(e.target.value);
            add.reset();
          }}
        />{' '}
        <button
          type="button"
          data-testid="person-alias-add"
          disabled={add.isPending}
          onClick={() => {
            if (!submit.begin()) return;
            rowAction.take();
            add.reset();
            add.mutate(alias); // a blank alias is refused by the server
          }}
        >
          Add alias
        </button>
        {add.isError && rowAction.isLatest && <span data-testid="person-error"> Could not add this alias.</span>}
      </td>
    </>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 5, Component: AliasesHead }),
  PersonRow.fill({ order: 5, Component: PersonAliases }),
];
