import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { peopleKey, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { claimsKey } from '../claims/claims';
import { useUser } from '../session/session';
import { useRowError } from '../people/rowError';

function MergeHead() {
  return <th>Merge</th>;
}

// A picker of another person on the sheet and a button that merges this person into them (a
// steward): their claims, aliases, patrons and pool memberships go to the other, and they leave the list.
function PersonMerge({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const people = usePeople(sheetId).data ?? [];
  const [into, setInto] = useState('');
  const [error, setError] = useRowError(person.id, 'merge');
  const once = useOnce();
  const merge = useMutation({
    mutationFn: (intoId: string) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/merge`,
        { intoId },
        user,
      ),
    // The moved claims are read before the merged person's row goes.
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: () => setError('That person could not be merged.'),
  });

  const submit = () => {
    setError('');
    once((done) => merge.mutate(into, { onSettled: done }));
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
        {people.map((p) => (
          <option key={p.id} value={p.id}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button type="button" data-testid="person-merge" onClick={submit} disabled={merge.isPending}>
        Merge
      </button>
      {error && (
        <p role="alert" data-testid="person-error">
          {error}
        </p>
      )}
    </td>
  );
}

function MergeUndoHead() {
  return <th>Undo merge</th>;
}

// A button that undoes the last merge into this person (a steward, on the sheet's today it was made):
// the merged person is back at the end of the list with the claims that came from them and their own aliases.
function PersonMergeUndo({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useRowError(person.id, 'merge-undo');
  const once = useOnce();
  const undo = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/merge-undo`,
        {},
        user,
      ),
    // The moved-back claims are read before the restored person's row shows.
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: () => setError('That merge could not be undone.'),
  });

  const submit = () => {
    setError('');
    once((done) => undo.mutate(undefined, { onSettled: done }));
  };

  return (
    <td>
      <button type="button" data-testid="person-merge-undo" onClick={submit} disabled={undo.isPending}>
        Undo merge
      </button>
      {error && (
        <p role="alert" data-testid="person-error">
          {error}
        </p>
      )}
    </td>
  );
}

export const contributions = [
  PersonHead.fill({ order: 65, Component: MergeHead }),
  PersonRow.fill({ order: 65, Component: PersonMerge }),
  PersonHead.fill({ order: 65.5, Component: MergeUndoHead }),
  PersonRow.fill({ order: 65.5, Component: PersonMergeUndo }),
];
