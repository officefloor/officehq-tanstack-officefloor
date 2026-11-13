import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { useCurrentSheet, useCurrentUser } from '../session/session';

// The buttons that swap the person with the one above or below them in the list. The top person
// cannot move up nor the bottom one down: the server refuses, and the row shows the error.
export function PersonMove({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const move = useMutation({
    mutationFn: (direction: 'up' | 'down') =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/move?direction=${direction}&user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
    onError: (_e, direction) =>
      setError(direction === 'up' ? 'The top person cannot move up.' : 'The bottom person cannot move down.'),
  });

  const submit = (direction: 'up' | 'down') => {
    setError('');
    once(() => move.mutateAsync(direction));
  };

  return (
    <td>
      <button type="button" data-testid="person-move-up" disabled={move.isPending} onClick={() => submit('up')}>
        Up
      </button>{' '}
      <button type="button" data-testid="person-move-down" disabled={move.isPending} onClick={() => submit('down')}>
        Down
      </button>
    </td>
  );
}
