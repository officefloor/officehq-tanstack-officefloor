import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';

// The select of the person to merge this one into and the button that merges them (a steward only; not into
// themselves, nor when either is retired). Their claims, name, aliases, patrons and pool memberships pass to the
// other and they leave the list, so everything on the sheet is re-read.
export function PersonMerge({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const [into, setInto] = useState('');
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const merge = useMutation({
    mutationFn: (id: number) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/merge?user=${encodeURIComponent(user)}`,
        { into: id },
      ),
    onSuccess: async () => {
      setInto('');
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
    },
    onError: () => setError('The person could not be merged.'),
  });

  const submit = () => {
    setError('');
    if (!into) {
      setError('Choose the person to merge into.');
      return;
    }
    once(() => merge.mutateAsync(Number(into)));
  };

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
        <option value="">—</option>
        {(people.data ?? []).map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>
      <button type="button" data-testid="person-merge" disabled={merge.isPending} onClick={submit}>
        Merge
      </button>
    </td>
  );
}
