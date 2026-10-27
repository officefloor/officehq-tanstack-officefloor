import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { usePersonError } from './usePersonError';
import { useUser } from '../session/useUser';
import { peopleQuery } from './peopleQuery';
import type { Person } from './peopleQuery';
import { sheetKey } from '../sheet/sheetQuery';

/**
 * The control that merges the person into another (a steward only; neither may be retired). The merged person's
 * claims, aliases, patrons and pool memberships go to the other and they leave the list, so every figure of the
 * sheet is re-read.
 */
function PersonMerge({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [into, setInto] = useState('');
  const [, setError] = usePersonError(personId);
  const once = useSubmitOnce();
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const merge = useMutation({
    mutationFn: (intoId: number) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/merge`, { intoId }, user),
    onSuccess: async () => {
      setInto('');
      setError('');
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheetId) });
    },
    onError: () => setError('The person could not be merged.'),
  });

  return (
    <td>
      <select
        data-testid="person-merge-into"
        value={into}
        onChange={(e) => {
          setInto(e.target.value);
          setError('');
        }}
      >
        <option value="">merge into…</option>
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
          setError('');
          if (into === '') {
            setError('Choose a person to merge into.');
            return;
          }
          once(() => merge.mutateAsync(Number(into)));
        }}
      >
        Merge
      </button>
    </td>
  );
}

// The row's `person-error` is shown by the hold cell, shared by every cell acting on the person.
export const contributions = [
  PersonHeaders.fill({ order: 23, Component: () => <th>Merge</th> }),
  PersonCells.fill({ order: 23, Component: PersonMerge }),
];
