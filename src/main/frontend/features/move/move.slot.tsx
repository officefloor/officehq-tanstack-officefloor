import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, type Person } from '../people/usePeople';
import { useRowAction } from '../people/useRowAction';

/**
 * The buttons to move a person up or down the list, swapping them with the one above or below.
 * The list, the rollup lines and every action that works in list order follow it. The top row
 * cannot move up, nor the bottom row down.
 */
function PersonMove({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const move = useMutation({
    mutationFn: (direction: 'up' | 'down') =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/move`, { direction, user }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });
  const actionTaken = useRowAction(sheet, person.id, move.reset);
  const press = (direction: 'up' | 'down') => {
    actionTaken();
    move.reset();
    move.mutate(direction);
  };
  return (
    <td>
      <button type="button" data-testid="person-move-up" disabled={move.isPending} onClick={() => press('up')}>
        Up
      </button>{' '}
      <button type="button" data-testid="person-move-down" disabled={move.isPending} onClick={() => press('down')}>
        Down
      </button>
      {move.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          Could not move this person.
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 2, Component: () => <th>Move</th> }),
  PersonCells.fill({ order: 2, Component: PersonMove }),
];
