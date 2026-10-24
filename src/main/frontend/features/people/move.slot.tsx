import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peoplePath, peopleQueryKey, type Person } from './people';

// The buttons that swap a person with the one above or below them in the list (retired people included). The top
// person cannot move up, nor the bottom one down. The list, the rollup lines and every action taken in list order
// follow the places.
function MoveHeader() {
  return <th>Move</th>;
}

function PersonMove({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();
  const move = useMutation({
    mutationFn: (body: { direction: 'up' | 'down' }) => putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/move`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
    },
  });
  const submit = useSubmitOnce(move);
  const rowAction = useRowAction(`person-${person.id}`);
  return (
    <td>
      <button type="button" data-testid="person-move-up" disabled={move.isPending}
        onClick={() => { rowAction.take(); submit({ direction: 'up' }); }}>
        Up
      </button>{' '}
      <button type="button" data-testid="person-move-down" disabled={move.isPending}
        onClick={() => { rowAction.take(); submit({ direction: 'down' }); }}>
        Down
      </button>
      {move.isError && rowAction.isLatest && <span data-testid="person-error">The person could not be moved.</span>}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 15, Component: MoveHeader }),
  PersonCells.fill({ order: 15, Component: PersonMove }),
];
