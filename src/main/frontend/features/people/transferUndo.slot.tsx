import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peoplePath, type Person } from './people';

// The button that undoes the transfer that brought this person here (a steward only, on the sheet's today they
// arrived): they are transferred back to the sheet they came from and leave this sheet's list.
function TransferUndoHeader() {
  return <th>Undo transfer</th>;
}

function PersonTransferUndo({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();
  const undo = useMutation({
    mutationFn: () => putJson<unknown>(`${peoplePath(sheet ?? '')}/${person.id}/transfer-undo`, {}, user),
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
        data-testid="person-transfer-undo"
        disabled={undo.isPending}
        onClick={() => { rowAction.take(); submit(undefined); }}
      >
        Undo transfer
      </button>
      {undo.isError && rowAction.isLatest && (
        <span data-testid="person-error">The transfer could not be undone.</span>
      )}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 50.5, Component: TransferUndoHeader }),
  PersonCells.fill({ order: 50.5, Component: PersonTransferUndo }),
];
