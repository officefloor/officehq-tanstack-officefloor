import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { usePersonError } from '../people/usePersonError';
import { useUser } from '../session/useUser';
import { watchesKey, watchesQuery } from './watchesQuery';
import type { Watch } from './watchesQuery';

/**
 * Whether the signed-in user watches the person, and the toggle that starts or stops it. When a watched
 * person's claim is settled, approved or re-settled, each watcher is notified (in the audit file).
 */
function PersonWatch({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = usePersonError(personId);
  const once = useSubmitOnce();
  const watches = useQuery(watchesQuery(sheetId)).data ?? [];
  const watched = watches.some((w) => w.personId === personId && w.user === (user ?? '').trim());
  const watch = useMutation({
    mutationFn: (on: boolean) =>
      putJson<Watch>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/watch`, { watched: on }, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: watchesKey(sheetId) }),
    onError: () => setError('The watch could not be changed.'),
  });

  return (
    <td>
      <span data-testid="person-watched">{watched ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="person-watch"
        disabled={watch.isPending}
        onClick={() => {
          setError('');
          once(() => watch.mutateAsync(!watched));
        }}
      >
        {watched ? 'Unwatch' : 'Watch'}
      </button>
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 19.5, Component: () => <th>Watched</th> }),
  PersonCells.fill({ order: 19.5, Component: PersonWatch }),
];
