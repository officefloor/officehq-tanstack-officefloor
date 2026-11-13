import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { isWatching, useWatches, watchesKey } from './watches';

// Whether the signed-in user watches the person, and the button that toggles it. A watcher is notified
// (in the audit) when one of the person's claims is settled, approved or re-settled.
export function PersonWatch({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const watched = isWatching(useWatches().data ?? [], person.id, user);
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const toggle = useMutation({
    mutationFn: () =>
      postJson<{ personId: number; watched: boolean }>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/watch?user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: watchesKey(sheet) });
    },
    onError: () => setError('The watch could not be changed.'),
  });

  return (
    <td>
      <span data-testid="person-watched">{watched ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="person-watch"
        disabled={toggle.isPending}
        onClick={() => {
          setError('');
          once(() => toggle.mutateAsync());
        }}
      >
        {watched ? 'Unwatch' : 'Watch'}
      </button>
    </td>
  );
}
