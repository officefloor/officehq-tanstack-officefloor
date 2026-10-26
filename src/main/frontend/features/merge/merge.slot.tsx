import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { usePeople, type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';
import { useUser } from '../session/useSession';

function MergeHeads() {
  return <th>Merge</th>;
}

// The picker of another person and the button that merges this person into them: a steward's only, never into the
// person themselves, and neither may be retired. The merged person's claims, names, patrons and pool memberships go to
// the other and their row leaves the list, so the whole sheet is re-read. A refusal shows on the person until the
// picker is changed or the next action on their row.
function MergeCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const people = usePeople(sheetId).data ?? [];
  const [chosen, setChosen] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const merge = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: async (into: string) => {
      if (into === '') throw new Error('Choose a person to merge into');
      return postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/merge`, {
        into: Number(into),
        by: user ?? '',
      });
    },
    onSuccess: async () => {
      setChosen('');
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
  });
  const last = useLastPersonAction(sheetId, personId, merge.submittedAt);
  const offered = people.filter((p) => p.id !== personId);

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
        {offered.map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button type="button" data-testid="person-merge" disabled={merge.isPending} onClick={() => merge.mutate(chosen)}>
        Merge
      </button>
      {merge.isError && last && (
        <span data-testid="person-error" role="alert">
          {merge.error.message}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 57, Component: MergeHeads }),
  PersonCells.fill({ order: 57, Component: MergeCells }),
];
