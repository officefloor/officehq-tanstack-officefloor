import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { useRowError } from '../people/rowError';
import { useWatches, watchesKey } from './watch';

function WatchHead() {
  return (
    <>
      <th>Watched</th>
      <th />
    </>
  );
}

// Whether the signed-in user watches the person, and a button that toggles it. A watched person's
// claim settled, approved or re-settled notifies each of their watchers.
function PersonWatch({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const watches = useWatches(sheetId, user);
  const watched = (watches.data ?? []).includes(person.id);
  const [error, setError] = useRowError(person.id, 'watch');
  const once = useOnce();
  const watch = useMutation({
    mutationFn: (on: boolean) =>
      postJson<{ personIds: string[] }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/watch`,
        { on },
        user,
      ),
    onSuccess: (data) => queryClient.setQueryData(watchesKey(sheetId, user), data.personIds),
    onError: () => setError('That person could not be watched.'),
  });

  const toggle = () => {
    setError('');
    once((done) => watch.mutate(!watched, { onSettled: done }));
  };

  return (
    <>
      <td data-testid="person-watched">{watched ? 'yes' : 'no'}</td>
      <td>
        <button
          type="button"
          data-testid="person-watch"
          onClick={toggle}
          disabled={watch.isPending || watches.data === undefined}
        >
          {watched ? 'Unwatch' : 'Watch'}
        </button>
        {error && (
          <p role="alert" data-testid="person-error">
            {error}
          </p>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 45, Component: WatchHead }),
  PersonRow.fill({ order: 45, Component: PersonWatch }),
];
