import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, personActionKey, useLatestPersonAction } from './people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';

function MoveHead() {
  return <th>Move</th>;
}

// The buttons that swap the person with the one above or below them in the list. The list, the
// rollup lines and every action that works in list order follow the move; the top row cannot move
// up nor the bottom row down. A refused move shows its error until the next action on the row.
function PersonMove({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  const move = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (direction: 'up' | 'down') =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/move`, { direction, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });

  const latest = useLatestPersonAction(sheet, person.id);

  return (
    <td>
      {(['up', 'down'] as const).map((direction) => (
        <button
          key={direction}
          type="button"
          data-testid={`person-move-${direction}`}
          aria-label={`Move ${direction}`}
          disabled={move.isPending}
          onClick={() => {
            move.reset();
            move.mutate(direction);
          }}
        >
          {direction === 'up' ? '↑' : '↓'}
        </button>
      ))}
      {move.isError && move.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {move.error instanceof HttpError && move.error.reason ? `${move.error.reason}.` : 'The move was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 0.5, Component: MoveHead }),
  PersonColumns.fill({ order: 0.5, Component: PersonMove }),
];
