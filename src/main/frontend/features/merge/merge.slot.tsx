import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/usePeople';
import { useRowAction } from '../people/useRowAction';
import { sheetKey } from '../sheet/useSheet';

/**
 * Merging a person into another: the other person and the button to merge. Only a steward may; the
 * server refuses the person themselves and either of them retired. The merged person's claims,
 * aliases, patrons and pool memberships move to the other, and they leave the list.
 */
function PersonMerge({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: people = [] } = usePeople(sheet);
  const [into, setInto] = useState('');
  const merge = useMutation({
    mutationFn: (intoId: number) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/merge`, { intoId, user }),
    // Everything else on the sheet first (the claims move with the person), then the list the row leaves.
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: sheetKey(sheet),
        predicate: (q) => q.queryKey[2] !== peopleKey(sheet)[2],
      }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });
  const actionTaken = useRowAction(sheet, person.id, merge.reset);
  return (
    <td>
      <select
        data-testid="person-merge-into"
        value={into}
        onChange={(e) => {
          merge.reset();
          setInto(e.target.value);
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
        disabled={merge.isPending || into === ''}
        onClick={() => {
          actionTaken();
          merge.reset();
          merge.mutate(Number(into));
        }}
      >
        Merge
      </button>
      {merge.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          Could not merge this person (a steward only, into someone else, neither of them retired).
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 45, Component: () => <th>Merge</th> }),
  PersonCells.fill({ order: 45, Component: PersonMerge }),
];
