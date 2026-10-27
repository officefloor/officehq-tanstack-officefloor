import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { usePersonError } from './usePersonError';
import { useUser } from '../session/useUser';
import { peopleKey, peopleQuery } from './peopleQuery';
import type { Person } from './peopleQuery';

/**
 * The person's aliases, sorted as names are compared, and the control that adds one. An alias shares the names'
 * uniqueness on the sheet: one equal (ignoring case) to any name or alias there is refused, as is a blank one.
 */
function PersonAliases({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [alias, setAlias] = useState('');
  const [, setError] = usePersonError(personId);
  const once = useSubmitOnce();
  const person = (useQuery(peopleQuery(sheetId)).data ?? []).find((p) => p.id === personId);
  const add = useMutation({
    mutationFn: (body: { alias: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/aliases`, body, user),
    onSuccess: async () => {
      setAlias('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: () => setError('The alias could not be added: it is blank or already in use.'),
  });

  return (
    <td>
      <span data-testid="person-aliases">{(person?.aliases ?? []).join(', ')}</span>{' '}
      <input
        data-testid="person-alias-input"
        placeholder="alias"
        value={alias}
        onChange={(e) => {
          setAlias(e.target.value);
          setError('');
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-alias-add"
        disabled={add.isPending}
        onClick={() => {
          setError('');
          const a = alias.trim();
          if (a === '') {
            setError('Type an alias.');
            return;
          }
          once(() => add.mutateAsync({ alias: a }));
        }}
      >
        Add alias
      </button>
    </td>
  );
}

// The row's `person-error` is shown by the hold cell, shared by every cell acting on the person.
export const contributions = [
  PersonHeaders.fill({ order: 9, Component: () => <th>Aliases</th> }),
  PersonCells.fill({ order: 9, Component: PersonAliases }),
];
