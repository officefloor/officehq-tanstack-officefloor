import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from './usePersonAction';
import { type Person } from './usePeople';

// Merges the person into another chosen by name (a steward only): their claims, names, patrons and
// pool memberships move to the other and they leave the list. Merging into themselves, or when either
// is retired, is refused. A refusal shows the person's error.
export function MergeControl({ personId, people }: { personId: string; people: Person[] }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [intoId, setIntoId] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const merge = useMutation({
    mutationFn: (into: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/merge`, {
        intoId: into,
        user,
      }),
    onSuccess: () => {
      setIntoId('');
      return queryClient.invalidateQueries({ queryKey: ['sheet', sheet] });
    },
  });
  const taken = usePersonAction(personId, () => merge.reset());

  return (
    <>
      <select
        data-testid="person-merge-into"
        value={intoId}
        onChange={(e) => {
          setIntoId(e.target.value);
          merge.reset();
        }}
      >
        <option value="">Merge into…</option>
        {people.map((p) => (
          <option key={p.id} value={p.id}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="person-merge"
        onClick={() => {
          taken();
          once(() => merge.mutateAsync(intoId));
        }}
      >
        Merge
      </button>
      {merge.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The person could not be merged.
        </span>
      )}
    </>
  );
}
