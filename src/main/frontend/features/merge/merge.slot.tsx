import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { usePeople, type Person } from '../people/usePeople';
import { usePersonRowAction } from '../people/usePersonRowAction';

/** A steward merges a person into another: a select of the other person and a merge button. The merged
 *  person's claims, name and aliases, patrons and pool memberships pass to the other and they leave the list,
 *  so every figure on the sheet is re-read. Anyone else, the same person, or either of them retired is
 *  refused with the row's error. */
function PersonMerge({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState('');

  const merge = useMutation({
    mutationFn: (intoId: number) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/merge`, { intoId }),
    onSuccess: async () => {
      setChosen('');
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
  });
  const announce = usePersonRowAction(sheetId, personId, merge.reset);

  return (
    <td>
      <select
        data-testid="person-merge-into"
        value={chosen}
        onChange={(e) => {
          setChosen(e.target.value);
          merge.reset();
        }}
      >
        <option value="">Merge into…</option>
        {(people.data ?? []).map((p) => (
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
          announce();
          merge.reset();
          merge.mutate(Number(chosen));
        }}
      >
        Merge
      </button>
      {merge.isError && <span data-testid="person-error">{merge.error.message}</span>}
    </td>
  );
}

/** The steward who made the last merge into a person undoes it on the same sheet-today: the merged person is back
 *  at the end of the list with the claims that came from them and their own aliases, so every figure on the sheet is
 *  re-read. Any later day, anyone else, or a person with no merge to undo is refused with the row's error. */
function PersonMergeUndo({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const undo = useMutation({
    mutationFn: () => postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/merge-undo`, {}),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
  });
  const announce = usePersonRowAction(sheetId, personId, undo.reset);

  return (
    <td>
      <button
        type="button"
        data-testid="person-merge-undo"
        disabled={undo.isPending}
        onClick={() => {
          announce();
          undo.reset();
          undo.mutate();
        }}
      >
        Undo merge
      </button>
      {undo.isError && <span data-testid="person-error">{undo.error.message}</span>}
    </td>
  );
}

export const contributions = [
  PersonColumnHead.fill({ order: 55, Component: () => <th>Merge</th> }),
  PersonCells.fill({ order: 55, Component: PersonMerge }),
  PersonColumnHead.fill({ order: 55.2, Component: () => <th>Undo merge</th> }),
  PersonCells.fill({ order: 55.2, Component: PersonMergeUndo }),
];
