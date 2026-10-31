import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, type Person } from '../people/usePeople';
import { useRowAction } from '../people/useRowAction';

/**
 * A person's aliases (sorted as names are compared, comma and space separated), and the entry and
 * button to add one, and whether the person clashes with the sheet's tree. Aliases share the names'
 * uniqueness: the server refuses a blank alias and one equal, as names are compared, to any name or
 * alias on the sheet or on another sheet of its tree (its ancestors and descendants).
 */
function PersonAliases({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const [alias, setAlias] = useState('');
  const add = useMutation({
    mutationFn: (a: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/aliases`, { alias: a, user }),
    onSuccess: () => setAlias(''),
    onSettled: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });
  const actionTaken = useRowAction(sheet, person.id, add.reset);
  return (
    <>
      <td data-testid="person-clashing">{person.clashing ? 'yes' : 'no'}</td>
      <td data-testid="person-aliases">{person.aliases.join(', ')}</td>
      <td>
        <input
          type="text"
          placeholder="alias"
          data-testid="person-alias-input"
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
            actionTaken();
            add.reset();
            add.mutate(alias);
          }}
        >
          Add alias
        </button>
        {add.isError && (
          <span data-testid="person-error" role="alert">
            {' '}
            Could not add this alias (not blank, and not a name or alias already on the sheet or its tree).
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 5,
    Component: () => (
      <>
        <th>Clashing</th>
        <th>Aliases</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 5, Component: PersonAliases }),
];
