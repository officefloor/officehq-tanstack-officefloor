import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { peopleKey } from './people';
import { usePersonError } from './rowError';
import type { Person } from './people';

/** The buttons that move the person up or down the list, swapping them with the one above or below. The top
 *  person cannot move up nor the bottom one down. */
function PersonMove({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const move = useMutation({
    mutationFn: (direction: 'up' | 'down') =>
      putJson(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/move`, { user, direction }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: async (e) => {
      setError(e instanceof HttpError && e.body ? e.body : `Could not move ${person.name}.`);
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onSettled: once.done,
  });

  const press = (direction: 'up' | 'down') => {
    begin();
    if (!once.start()) return;
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
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 5, Component: PersonMove });
