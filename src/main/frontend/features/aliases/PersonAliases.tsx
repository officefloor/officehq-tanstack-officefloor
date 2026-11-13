import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { aliasesKey, aliasesOf, useAliases } from './aliases';
import type { Alias } from './aliases';

// A person's aliases (sorted as names are compared) and the entry and button that add one more. An alias shares
// the names' uniqueness: one equal to any name or alias on the sheet, or a blank one, is refused on the row.
export function PersonAliases({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const aliases = aliasesOf(useAliases().data ?? [], person.id);
  const [typed, setTyped] = useState('');
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (alias: string) =>
      postJson<Alias>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/aliases?user=${encodeURIComponent(user)}`,
        { alias },
      ),
    onSuccess: async () => {
      setTyped('');
      await queryClient.invalidateQueries({ queryKey: aliasesKey(sheet) });
    },
    onError: () => setError('The alias could not be added.'),
  });

  const submit = () => {
    setError('');
    const alias = typed.trim();
    if (!alias) {
      setError('Type an alias.');
      return;
    }
    once(() => add.mutateAsync(alias));
  };

  return (
    <td>
      <span data-testid="person-aliases">{aliases.join(', ')}</span>{' '}
      <input
        data-testid="person-alias-input"
        value={typed}
        onChange={(e) => {
          setTyped(e.target.value);
          setError('');
        }}
      />
      <button type="button" data-testid="person-alias-add" disabled={add.isPending} onClick={submit}>
        Add alias
      </button>
    </td>
  );
}
