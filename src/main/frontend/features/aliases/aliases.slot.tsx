import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { useRowError } from '../people/rowError';
import { useUser } from '../session/session';

function AliasesHead() {
  return (
    <>
      <th>Aliases</th>
      <th />
    </>
  );
}

// Names compare ignoring case and surrounding spaces.
const asName = (s: string) => s.trim().toLowerCase();

// A person's aliases (sorted as names are compared, comma and space separated), and an entry that
// gives them one more. The quick-add bar and the paste box find a person by an alias as by the name.
function PersonAliases({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [alias, setAlias] = useState('');
  const [error, setError] = useRowError(person.id, 'alias');
  const once = useOnce();

  const shown = [...person.aliases]
    .sort((a, b) => {
      const x = asName(a);
      const y = asName(b);
      return x < y ? -1 : x > y ? 1 : 0;
    })
    .join(', ');

  const add = useMutation({
    mutationFn: (value: string) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/aliases`,
        { alias: value },
        user,
      ),
    onSuccess: () => {
      setAlias('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: () => setError('That alias could not be added.'),
  });

  const submit = () => {
    setError('');
    once((done) => add.mutate(alias, { onSettled: done }));
  };

  return (
    <>
      <td data-testid="person-aliases">{shown}</td>
      <td>
        <input
          data-testid="person-alias-input"
          value={alias}
          onChange={(e) => {
            setAlias(e.target.value);
            setError('');
          }}
        />
        <button type="button" data-testid="person-alias-add" onClick={submit} disabled={add.isPending}>
          Add alias
        </button>
        {error && (
          <p role="alert" data-testid="person-error">
            {error}
          </p>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 55, Component: AliasesHead }),
  PersonRow.fill({ order: 55, Component: PersonAliases }),
];
