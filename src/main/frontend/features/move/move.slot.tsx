import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { peopleKey, type Person } from '../people/usePeople';
import { usePersonRowAction } from '../people/usePersonRowAction';

/** Up and down buttons that swap the person with the one above or below them in the sheet's list (the top
 *  person cannot move up nor the bottom one down: refused, with the row's error). */
function PersonMove({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const move = useMutation({
    mutationFn: (direction: 'up' | 'down') =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/move`, { direction }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });
  const announce = usePersonRowAction(sheetId, personId, move.reset);
  const press = (direction: 'up' | 'down') => {
    announce();
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
      {move.isError && <span data-testid="person-error">{move.error.message}</span>}
    </td>
  );
}

export const contributions = [
  PersonColumnHead.fill({ order: 0.5, Component: () => <th>Move</th> }),
  PersonCells.fill({ order: 0.5, Component: PersonMove }),
];
