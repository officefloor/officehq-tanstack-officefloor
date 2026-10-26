import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { peopleKey, type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';
import { useUser } from '../session/useSession';

function MoveHeads() {
  return <th>Move</th>;
}

// The buttons that swap the person with the one above or below them in the list. The top row cannot move up nor
// the bottom row down: refused, the error shows on the person until the next action on their row.
function MoveCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const [user] = useUser();
  const queryClient = useQueryClient();
  const move = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: (direction: 'up' | 'down') =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/move`, {
        direction,
        by: user ?? '',
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });
  const last = useLastPersonAction(sheetId, personId, move.submittedAt);

  return (
    <td>
      <button type="button" data-testid="person-move-up" disabled={move.isPending} onClick={() => move.mutate('up')}>
        Up
      </button>{' '}
      <button type="button" data-testid="person-move-down" disabled={move.isPending} onClick={() => move.mutate('down')}>
        Down
      </button>
      {move.isError && last && (
        <span data-testid="person-error" role="alert">
          {move.error.message}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 5, Component: MoveHeads }),
  PersonCells.fill({ order: 5, Component: MoveCells }),
];
