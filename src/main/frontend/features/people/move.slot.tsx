import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useIsLatest } from '../../query/useIsLatest';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, peoplePath, personActionKey } from './people';
import type { Person } from './people';

type Direction = 'up' | 'down';

function MoveHeader() {
  return <th>Move</th>;
}

// The buttons that swap the person with the one above or below them in the list. The top row cannot move up nor
// the bottom row down: the server refuses it and the row shows the error.
function PersonMove({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const move = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (direction: Direction) =>
      postJson<Person[]>(byUser(`${peoplePath(sheet)}/${person.id}/move`, user), { direction }),
    onSuccess: (updated) => {
      queryClient.setQueryData(peopleKey(sheet), updated);
    },
  });
  const act = useSubmitOnce(move);
  const latest = useIsLatest(personActionKey(sheet, person.id), move.submittedAt);

  return (
    <td>
      <button type="button" data-testid="person-move-up" disabled={move.isPending} onClick={() => act('up')}>
        Up
      </button>{' '}
      <button type="button" data-testid="person-move-down" disabled={move.isPending} onClick={() => act('down')}>
        Down
      </button>
      {move.isError && latest && (
        <span data-testid="person-error" role="alert">
          {' '}That move was refused: the top row cannot move up nor the bottom row down.
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 2, Component: MoveHeader }),
  PersonColumns.fill({ order: 2, Component: PersonMove }),
];
