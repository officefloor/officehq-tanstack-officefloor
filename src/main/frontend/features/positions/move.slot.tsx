import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { useRowError } from '../people/rowError';

type Direction = 'up' | 'down';

function MoveHead() {
  return <th>Move</th>;
}

// Up and down buttons that swap the person with the one above or below them in the list. The top
// row cannot move up nor the bottom row down: the server refuses it and the row shows the error.
function PersonMove({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useRowError(person.id, 'move');
  const once = useOnce();
  const move = useMutation({
    mutationFn: (direction: Direction) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/move`,
        { direction },
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
    onError: (_e, direction) =>
      setError(direction === 'up' ? 'The top row cannot move up.' : 'The bottom row cannot move down.'),
  });

  const submit = (direction: Direction) => {
    setError('');
    once((done) => move.mutate(direction, { onSettled: done }));
  };

  return (
    <td>
      <button type="button" data-testid="person-move-up" onClick={() => submit('up')} disabled={move.isPending}>
        Up
      </button>{' '}
      <button type="button" data-testid="person-move-down" onClick={() => submit('down')} disabled={move.isPending}>
        Down
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
  PersonHead.fill({ order: 2, Component: MoveHead }),
  PersonRow.fill({ order: 2, Component: PersonMove }),
];
