import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { peopleKey, type Person } from '../people/people';
import { usePersonRowAction } from '../people/rowAction';

function MoveHead() {
  return <th>Move</th>;
}

/** The buttons that swap the person with the one above or below them in the list (the list, the rollup lines and
 *  every action taken in list order follow it). Moving the top person up or the bottom one down is refused, showing
 *  the row's error. */
function PersonMove({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const submit = useSubmitOnce();
  const rowAction = usePersonRowAction(sheetId, person.id, 'move');
  const move = useMutation({
    mutationFn: (direction: 'up' | 'down') =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/move`, { direction, by: user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
    onSettled: submit.end,
  });
  const press = (direction: 'up' | 'down') => {
    if (!submit.begin()) return;
    rowAction.take();
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
      {move.isError && rowAction.isLatest && <span data-testid="person-error"> Could not move this person.</span>}
    </td>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 2, Component: MoveHead }),
  PersonRow.fill({ order: 2, Component: PersonMove }),
];
