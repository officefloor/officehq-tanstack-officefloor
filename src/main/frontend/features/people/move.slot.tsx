import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { usePersonError } from './usePersonError';
import { useUser } from '../session/useUser';
import { peopleKey } from './peopleQuery';
import type { Person } from './peopleQuery';

/**
 * The controls that move the person up or down the list, swapping them with the person above or
 * below. The top person cannot move up nor the bottom person down: the server refuses it.
 */
function PersonMove({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = usePersonError(personId);
  const once = useSubmitOnce();
  const move = useMutation({
    mutationFn: (direction: 'up' | 'down') =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/move/${direction}`, {}, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: (_e, direction) => setError(`The person could not be moved ${direction}.`),
  });

  const button = (direction: 'up' | 'down', label: string) => (
    <button
      type="button"
      data-testid={`person-move-${direction}`}
      disabled={move.isPending}
      onClick={() => {
        setError('');
        once(() => move.mutateAsync(direction));
      }}
    >
      {label}
    </button>
  );

  return (
    <td>
      {button('up', 'Up')} {button('down', 'Down')}
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 24, Component: () => <th>Move</th> }),
  PersonCells.fill({ order: 24, Component: PersonMove }),
];
