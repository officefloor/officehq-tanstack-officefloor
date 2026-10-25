import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, putJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';

export const watchesKey = (sheetId: string, user: string) => ['sheet', sheetId, 'watches', user] as const;

/** The ids of the people on the sheet the user watches. */
export function useWatches(sheetId: string, user: string) {
  return useQuery({
    queryKey: watchesKey(sheetId, user),
    queryFn: () =>
      getJson<number[]>(`/api/sheets/${encodeURIComponent(sheetId)}/watches/${encodeURIComponent(user)}`),
  });
}

function WatchHeads() {
  return (
    <>
      <th>Watched</th>
      <th />
    </>
  );
}

// Whether the signed-in user watches the person, and the button that starts or stops it. Watching is each user's
// own: a watched person's claim settled, approved or re-settled notifies every user watching them. A refusal shows
// on the person until the next action on their row.
function WatchCells({ sheetId, user, personId }: { sheetId: string; user: string; personId: number }) {
  const watched = (useWatches(sheetId, user).data ?? []).includes(personId);
  const queryClient = useQueryClient();
  const watch = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: (on: boolean) =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/watch`, { on, by: user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: watchesKey(sheetId, user) });
    },
  });
  const last = useLastPersonAction(sheetId, personId, watch.submittedAt);

  return (
    <>
      <td data-testid="person-watched">{watched ? 'yes' : 'no'}</td>
      <td>
        <button type="button" data-testid="person-watch" disabled={watch.isPending} onClick={() => watch.mutate(!watched)}>
          {watched ? 'Unwatch' : 'Watch'}
        </button>
        {watch.isError && last && (
          <span data-testid="person-error" role="alert">
            {watch.error.message}
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 47, Component: WatchHeads }),
  PersonCells.fill({ order: 47, Component: WatchCells }),
];
