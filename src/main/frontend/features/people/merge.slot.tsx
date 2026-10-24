import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peoplePath, usePeople, type Person } from './people';

// The person to merge this one into, and the button that merges them (a steward only; not into themselves, and
// neither of them retired). The merged person's claims, names, patrons and pool memberships go to the other, and
// they leave the list.
function MergeHeader() {
  return <th>Merge</th>;
}

function PersonMerge({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const [chosen, setChosen] = useState('');
  const queryClient = useQueryClient();
  const merge = useMutation({
    mutationFn: (body: { intoId: number }) =>
      putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/merge`, body, user),
    onSuccess: async () => {
      setChosen('');
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheet ?? ''] });
    },
  });
  const submit = useSubmitOnce(merge);
  const rowAction = useRowAction(`person-${person.id}`);
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
        <option value="">Merge into</option>
        {(people.data ?? []).map((p) => (
          <option key={p.id} value={String(p.id)}>{p.name}</option>
        ))}
      </select>
      <button
        type="button"
        data-testid="person-merge"
        disabled={merge.isPending}
        onClick={() => { rowAction.take(); submit({ intoId: Number(chosen) }); }}
      >
        Merge
      </button>
      {merge.isError && rowAction.isLatest && <span data-testid="person-error">The person could not be merged.</span>}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 49, Component: MergeHeader }),
  PersonCells.fill({ order: 49, Component: PersonMerge }),
];
