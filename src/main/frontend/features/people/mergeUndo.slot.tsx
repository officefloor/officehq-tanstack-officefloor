import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peoplePath, type Person } from './people';

// The button that undoes the last merge into this person (the steward who made it, on the sheet's today it was
// made): the merged person is back at the end of the list with the claims that came from them and their aliases.
function MergeUndoHeader() {
  return <th>Undo merge</th>;
}

function PersonMergeUndo({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();
  const undo = useMutation({
    mutationFn: () => putJson<unknown>(`${peoplePath(sheet ?? '')}/${person.id}/merge-undo`, {}, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheet ?? ''] });
    },
  });
  const submit = useSubmitOnce(undo);
  const rowAction = useRowAction(`person-${person.id}`);
  return (
    <td>
      <button
        type="button"
        data-testid="person-merge-undo"
        disabled={undo.isPending}
        onClick={() => { rowAction.take(); submit(undefined); }}
      >
        Undo merge
      </button>
      {undo.isError && rowAction.isLatest && (
        <span data-testid="person-error">The merge could not be undone.</span>
      )}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 49.5, Component: MergeUndoHeader }),
  PersonCells.fill({ order: 49.5, Component: PersonMergeUndo }),
];
