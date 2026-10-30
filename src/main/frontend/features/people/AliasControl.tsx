import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from './usePersonAction';
import { peopleKey, type Person } from './usePeople';

// Gives the person an alias, one at a time. A blank alias, or one equal (as names are compared) to
// any name or alias on the sheet, is refused. A refusal shows the person's error.
export function AliasControl({ personId }: { personId: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [alias, setAlias] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (value: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/aliases`, {
        alias: value,
        user,
      }),
    onSuccess: () => {
      setAlias('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });
  const taken = usePersonAction(personId, () => add.reset());

  return (
    <>
      <input
        data-testid="person-alias-input"
        value={alias}
        placeholder="Alias"
        onChange={(e) => {
          setAlias(e.target.value);
          add.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-alias-add"
        onClick={() => {
          taken();
          once(() => add.mutateAsync(alias));
        }}
      >
        Add alias
      </button>
      {add.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The alias could not be added.
        </span>
      )}
    </>
  );
}
