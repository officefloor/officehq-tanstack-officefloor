import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { peopleKey, useWatched, type Person } from '../people/usePeople';
import { usePersonRowAction } from '../people/usePersonRowAction';

/** Whether the signed-in user watches the person (yes/no), and a button that toggles it. A watcher is told
 *  (server side) when one of the person's claims is settled, approved or re-settled. */
function PersonWatch({ sheetId, personId }: { sheetId: string; personId: number }) {
  const watchedIds = useWatched(sheetId);
  const queryClient = useQueryClient();
  const watched = (watchedIds.data ?? []).includes(personId);

  const watch = useMutation({
    mutationFn: (body: { watch: boolean }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/watch`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });
  const announce = usePersonRowAction(sheetId, personId, watch.reset);

  return (
    <td>
      <span data-testid="person-watched">{watched ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="person-watch"
        disabled={watch.isPending || !watchedIds.isSuccess}
        onClick={() => {
          announce();
          watch.reset();
          watch.mutate({ watch: !watched });
        }}
      >
        {watched ? 'Unwatch' : 'Watch'}
      </button>
      {watch.isError && <span data-testid="person-error">{watch.error.message}</span>}
    </td>
  );
}

export const contributions = [
  PersonColumnHead.fill({ order: 55, Component: () => <th>Watched</th> }),
  PersonCells.fill({ order: 55, Component: PersonWatch }),
];
