import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import type { Person } from '../people/usePeople';
import { useRowAction } from '../people/useRowAction';
import { asOfKey, asOfPath, useAsOf } from '../asof/useAsOf';

const watchesKey = (sheet: string, user: string) => ['sheet', sheet, 'watches', user] as const;

/** The ids of the people on the sheet the signed-in user watches, re-read whenever the sheet is loaded. */
function useWatches(sheet: string, user: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(watchesKey(sheet, user), asOf),
    queryFn: () =>
      getJson<number[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/watches?user=${encodeURIComponent(user)}`, asOf)),
    staleTime: 0,
    gcTime: 0,
  });
}

/**
 * Whether the signed-in user watches the person, and the button to start or stop. Watches are per
 * user: each sees only their own. Settling, approving or re-settling a watched person's claim
 * notifies everyone watching them.
 */
function PersonWatch({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const watches = useWatches(sheet, user);
  const watch = useMutation({
    mutationFn: () =>
      postJson<{ personId: number; watched: boolean }>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/watch`,
        { user },
      ),
    onSettled: () => queryClient.invalidateQueries({ queryKey: watchesKey(sheet, user) }),
  });
  const actionTaken = useRowAction(sheet, person.id, watch.reset);
  const watched = watches.data?.includes(person.id) ?? false;
  return (
    <>
      <td data-testid="person-watched">{watches.data ? (watched ? 'yes' : 'no') : ''}</td>
      <td>
        <button
          type="button"
          data-testid="person-watch"
          disabled={watch.isPending}
          onClick={() => {
            actionTaken();
            watch.mutate();
          }}
        >
          {watched ? 'Unwatch' : 'Watch'}
        </button>
        {watch.isError && (
          <span data-testid="person-error" role="alert">
            {' '}
            Could not change the watch.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 37,
    Component: () => (
      <>
        <th>Watched</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 37, Component: PersonWatch }),
];
