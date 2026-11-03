import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { peopleKey, useAliases, type Person } from '../people/usePeople';
import { usePersonRowAction } from '../people/usePersonRowAction';

/** Sort as names are compared: ignoring case. */
function byName(a: string, b: string): number {
  const x = a.toLowerCase();
  const y = b.toLowerCase();
  return x < y ? -1 : x > y ? 1 : a < b ? -1 : a > b ? 1 : 0;
}

/** A person's aliases (sorted as names are compared, comma and space separated), and an entry and button
 *  that add one more. A blank alias, or one equal (ignoring case) to any name or alias on the sheet, is refused. */
function PersonAliases({ sheetId, personId }: { sheetId: string; personId: number }) {
  const aliases = useAliases(sheetId);
  const queryClient = useQueryClient();
  const [alias, setAlias] = useState('');

  const names = (aliases.data ?? [])
    .filter((a) => a.personId === personId)
    .map((a) => a.alias)
    .sort(byName);

  const add = useMutation({
    mutationFn: (raw: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/aliases`, { alias: raw.trim() }),
    onSuccess: async () => {
      setAlias('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });
  const announce = usePersonRowAction(sheetId, personId, add.reset);

  return (
    <>
      <td data-testid="person-aliases">{names.join(', ')}</td>
      <td>
        <input
          type="text"
          placeholder="Alias"
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
            announce();
            add.reset();
            add.mutate(alias);
          }}
        >
          Add alias
        </button>
        {add.isError && <span data-testid="person-error">{add.error.message}</span>}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHead.fill({
    order: 3,
    Component: () => (
      <>
        <th>Aliases</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 3, Component: PersonAliases }),
];
