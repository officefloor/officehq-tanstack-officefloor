import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { useCurrentSheet, useCurrentUser } from '../session/session';

// Whether the person is on hold, and the button that puts them on hold or takes them off it. While
// held, their open claims count in no live total; their settled claims still do.
export function PersonHold({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const hold = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/hold?user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
    onError: () => setError(person.held ? 'The person could not be released.' : 'The person could not be held.'),
  });

  const submit = () => {
    setError('');
    once(() => hold.mutateAsync());
  };

  return (
    <td>
      <span data-testid="person-hold">{person.held ? 'yes' : 'no'}</span>{' '}
      <button type="button" data-testid="person-hold-toggle" disabled={hold.isPending} onClick={submit}>
        {person.held ? 'Release' : 'Hold'}
      </button>
    </td>
  );
}
