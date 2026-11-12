import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';

function HoldHead() {
  return <th>Hold</th>;
}

// Whether the person is on hold, and the button that puts them on it or takes them off it. While
// held, their OPEN claims count in no live total. A refused toggle shows its error until the next.
function PersonHold({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  const hold = useMutation({
    mutationFn: () => postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/hold`, { user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });

  return (
    <td>
      <span data-testid="person-hold">{person.held ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="person-hold-toggle"
        disabled={hold.isPending}
        onClick={() => {
          hold.reset();
          hold.mutate();
        }}
      >
        {person.held ? 'Release' : 'Hold'}
      </button>
      {hold.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          {hold.error instanceof HttpError && hold.error.reason ? `${hold.error.reason}.` : 'The hold was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 19.5, Component: HoldHead }),
  PersonColumns.fill({ order: 19.5, Component: PersonHold }),
];
