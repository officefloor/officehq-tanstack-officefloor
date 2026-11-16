import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { usePeople, type Person } from '../people/people';
import { usePersonRowAction } from '../people/rowAction';

function MergeHead() {
  return <th>Merge</th>;
}

/** The picker of the person to merge this one into, and the button that merges them (a steward only). The merged
 *  person's claims, names, patrons and pool memberships go to the other and they leave the list, so every part of the
 *  sheet is re-read. A merge by anyone else, into the same person, or with either of them retired, shows the row's error. */
function PersonMerge({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId);
  const [into, setInto] = useState('');
  const submit = useSubmitOnce();
  const rowAction = usePersonRowAction(sheetId, person.id, 'merge');
  const merge = useMutation({
    mutationFn: (intoId: number) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/merge`, { intoId, by: user }),
    onSuccess: () => {
      setInto('');
      return queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onSettled: submit.end,
  });

  return (
    <td>
      <select
        data-testid="person-merge-into"
        value={into}
        onChange={(e) => {
          setInto(e.target.value);
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
          if (!submit.begin()) return;
          rowAction.take();
          merge.reset();
          merge.mutate(into === '' ? 0 : Number(into)); // nothing chosen: no person 0, so the server refuses it
        }}
      >
        Merge
      </button>
      {merge.isError && rowAction.isLatest && <span data-testid="person-error"> Could not merge this person.</span>}
    </td>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 80, Component: MergeHead }),
  PersonRow.fill({ order: 80, Component: PersonMerge }),
];
