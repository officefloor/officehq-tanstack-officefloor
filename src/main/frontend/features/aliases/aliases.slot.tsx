import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, personActionKey, useLatestPersonAction } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';

function AliasesHead() {
  return <th>Aliases</th>;
}

// The person's aliases (sorted as names are compared, comma and space separated), and the entry and
// button that add one. The quick-add bar and the paste box find a person by an alias as by their name.
// A refused alias shows its error until the entry is changed or the next action is taken.
function PersonAliases({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [alias, setAlias] = useState('');
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (typed: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/aliases`, { alias: typed, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      setAlias('');
    },
  });

  const latest = useLatestPersonAction(sheet, person.id);

  return (
    <td>
      <span data-testid="person-aliases">{person.aliases.join(', ')}</span>
      <input
        data-testid="person-alias-input"
        aria-label="Alias"
        value={alias}
        onChange={(e) => {
          add.reset();
          setAlias(e.target.value);
        }}
      />
      <button
        type="button"
        data-testid="person-alias-add"
        disabled={add.isPending}
        onClick={() => {
          add.reset();
          add.mutate(alias);
        }}
      >
        Add alias
      </button>
      {add.isError && add.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {add.error instanceof HttpError && add.error.reason ? `${add.error.reason}.` : 'The alias was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 1, Component: AliasesHead }),
  PersonColumns.fill({ order: 1, Component: PersonAliases }),
];
