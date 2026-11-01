import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useIsLatest } from '../../query/useIsLatest';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, peoplePath, personActionKey, usePeople } from './people';
import type { Person } from './people';

function MergeHeader() {
  return <th />;
}

// The person picked to merge this one into, and the button that merges them. Only a steward may; the server refuses
// the same person, or either of them retired. A merge moves claims, aliases, patrons and pool memberships to the
// other, so everything on the sheet is re-read before the merged person's row goes.
function PersonMerge({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: everyone = [] } = usePeople(sheet);
  const [into, setInto] = useState('');
  const merge = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (to: number) =>
      postJson<Person[]>(byUser(`${peoplePath(sheet)}/${person.id}/merge`, user), { into: to }),
    onSuccess: async (updated) => {
      await queryClient.invalidateQueries({
        predicate: (q) => q.queryKey.includes(sheet) && q.queryKey[0] !== 'sheet' && q.queryKey[0] !== 'people',
      });
      queryClient.setQueryData(peopleKey(sheet), updated);
      setInto('');
    },
  });
  const act = useSubmitOnce(merge);
  const latest = useIsLatest(personActionKey(sheet, person.id), merge.submittedAt);

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
        <option value="">merge into…</option>
        {everyone.map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="person-merge"
        disabled={merge.isPending}
        onClick={() => act(into === '' ? -1 : Number(into))}
      >
        Merge
      </button>
      {merge.isError && latest && (
        <span data-testid="person-error" role="alert">
          {' '}Only a steward may merge a person into another, neither retired.
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 70, Component: MergeHeader }),
  PersonColumns.fill({ order: 70, Component: PersonMerge }),
];
