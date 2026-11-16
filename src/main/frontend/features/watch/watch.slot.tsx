import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { type Person } from '../people/people';
import { usePersonRowAction } from '../people/rowAction';
import { useWatches, watchesKey, type Watch } from './watch';

function WatchHead() {
  return (
    <>
      <th>Watched</th>
      <th />
    </>
  );
}

/** Whether the signed-in user watches the person, and the button that starts or stops them watching. A settle,
 *  approve or re-settle of a watched person's claim notifies each watcher (in the audit file). A refused change shows
 *  the row's error. */
function PersonWatch({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const watches = useWatches(sheetId);
  const submit = useSubmitOnce();
  const rowAction = usePersonRowAction(sheetId, person.id, 'watch');
  const watched = (watches.data ?? []).some((w) => w.personId === person.id && w.user === user);
  const watch = useMutation({
    mutationFn: (on: boolean) =>
      postJson<Watch>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/watch`, { on, by: user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: watchesKey(sheetId) }),
    onSettled: submit.end,
  });

  return (
    <>
      <td data-testid="person-watched">{watched ? 'yes' : 'no'}</td>
      <td>
        <button
          type="button"
          data-testid="person-watch"
          disabled={watch.isPending || !watches.isSuccess}
          onClick={() => {
            if (!submit.begin()) return;
            rowAction.take();
            watch.reset();
            watch.mutate(!watched);
          }}
        >
          {watched ? 'Unwatch' : 'Watch'}
        </button>
        {watch.isError && rowAction.isLatest && <span data-testid="person-error"> Could not change watching this person.</span>}
      </td>
    </>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 70, Component: WatchHead }),
  PersonRow.fill({ order: 70, Component: PersonWatch }),
];
