import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { usePeople } from './people';
import { usePersonError } from './rowError';
import type { Person } from './people';

/** The picker that merges the person into another (a steward only; not into themselves, neither of them retired).
 *  Their claims, aliases, patrons and pool memberships move to the other, and they leave the list. */
function PersonMerge({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId).data ?? [];
  const [chosen, setChosen] = useState('');
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const merge = useMutation({
    mutationFn: (intoId: number) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/merge`, { user, intoId }),
    onSuccess: async () => {
      // the merge moves claims, aliases, patrons and pool members: everything shown of the sheet follows
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not merge ${person.name}.`),
    onSettled: once.done,
  });

  return (
    <td>
      <select
        data-testid="person-merge-into"
        value={chosen}
        onChange={(e) => {
          setChosen(e.target.value);
          setError('');
        }}
      >
        <option value="">Merge into…</option>
        {people.map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="person-merge"
        disabled={merge.isPending}
        onClick={() => {
          begin();
          if (chosen === '') {
            setError('Choose a person to merge into.');
            return;
          }
          if (!once.start()) return;
          merge.mutate(Number(chosen));
        }}
      >
        Merge
      </button>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 48, Component: PersonMerge });
