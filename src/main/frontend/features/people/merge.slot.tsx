import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, personActionKey, useLatestPersonAction, usePeople } from './people';
import { claimsKey } from '../claims/claims';
import { poolsKey } from '../pools/pools';
import { sheetKey } from '../sheet/sheet';
import { useCurrentSheet, useCurrentUser } from '../../session/session';

function MergeHead() {
  return <th>Merge</th>;
}

// The person to merge this one into (any person on the sheet), and the button that merges them: a
// steward's action. The merged person's claims, aliases, patrons and pool memberships move to the
// other and their row leaves the list. A refused merge shows its error until the next action is taken.
function PersonMerge({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet).data ?? [];
  const [into, setInto] = useState('');
  const queryClient = useQueryClient();

  const merge = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (intoId: number | null) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/merge`, { into: intoId, user }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: [...sheetKey(sheet), 'watches'] }),
      ]);
      setInto('');
    },
  });

  const latest = useLatestPersonAction(sheet, person.id);

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
      </select>
      <button
        type="button"
        data-testid="person-merge"
        disabled={merge.isPending}
        onClick={() => {
          merge.reset();
          merge.mutate(into === '' ? null : Number(into));
        }}
      >
        Merge
      </button>
      {merge.isError && merge.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {merge.error instanceof HttpError && merge.error.reason ? `${merge.error.reason}.` : 'The merge was refused.'}
        </span>
      )}
    </td>
  );
}

function MergeUndoHead() {
  return <th>Undo merge</th>;
}

// The button that undoes the last merge into this person: the action of the steward who merged, on the
// sheet-today it was made. The merged person is back at the end of the list with the claims that came
// from them and their own aliases. A refused undo shows its error until the next action is taken.
function PersonMergeUndo({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  const undo = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/merge-undo`, { user }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: [...sheetKey(sheet), 'watches'] }),
      ]);
    },
  });

  const latest = useLatestPersonAction(sheet, person.id);

  return (
    <td>
      <button
        type="button"
        data-testid="person-merge-undo"
        disabled={undo.isPending}
        onClick={() => {
          undo.reset();
          undo.mutate();
        }}
      >
        Undo merge
      </button>
      {undo.isError && undo.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {undo.error instanceof HttpError && undo.error.reason ? `${undo.error.reason}.` : 'The merge undo was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 19.9, Component: MergeHead }),
  PersonColumns.fill({ order: 19.9, Component: PersonMerge }),
  PersonColumnHeads.fill({ order: 19.91, Component: MergeUndoHead }),
  PersonColumns.fill({ order: 19.91, Component: PersonMergeUndo }),
];
