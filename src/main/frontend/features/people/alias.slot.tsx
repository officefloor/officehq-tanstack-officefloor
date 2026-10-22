import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { peopleKey } from './people';
import { usePersonError } from './rowError';
import type { Person } from './people';

/** The person's aliases, sorted as names are compared, and the form that gives them another. An alias shares the
 *  names' uniqueness: it must not equal, as names are compared, any name or alias on the sheet. The quick-add bar and
 *  the paste box find a person by an alias as well as by their name. */
function PersonAliases({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [alias, setAlias] = useState('');
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (body: { user: string; alias: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/aliases`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      setAlias('');
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not give ${person.name} an alias: an alias is required.`),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    begin();
    if (alias.trim() === '') {
      setError('An alias is required.');
      return;
    }
    if (!once.start()) return;
    add.mutate({ user, alias: alias.trim() });
  };

  return (
    <td>
      Aliases <span data-testid="person-aliases">{(person.aliases ?? []).join(', ')}</span>
      <form onSubmit={submit} style={{ display: 'inline' }}>
        {' '}
        <input
          data-testid="person-alias-input"
          placeholder="alias"
          value={alias}
          onChange={(e) => {
            setAlias(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="submit" data-testid="person-alias-add" disabled={add.isPending}>
          Add alias
        </button>
      </form>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 5, Component: PersonAliases });
