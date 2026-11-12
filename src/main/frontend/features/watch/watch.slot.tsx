import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, personActionKey, useLatestPersonAction } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { useWatches, watchesKey } from './watch';

function WatchHead() {
  return <th>Watched</th>;
}

// Whether the signed-in user watches the person, and the button that starts or stops it. A watcher is
// told of each claim of theirs settled, approved or re-settled. A refused toggle shows its error until
// the next action on the row is taken.
function PersonWatch({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const watches = useWatches(sheet, user);
  const queryClient = useQueryClient();

  const toggle = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: () => postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/watch`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: watchesKey(sheet, user) });
    },
  });

  const latest = useLatestPersonAction(sheet, person.id);
  const watched = (watches.data ?? []).includes(person.id);

  return (
    <td>
      <span data-testid="person-watched">{watched ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="person-watch"
        disabled={toggle.isPending}
        onClick={() => {
          toggle.reset();
          toggle.mutate();
        }}
      >
        {watched ? 'Unwatch' : 'Watch'}
      </button>
      {toggle.isError && toggle.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {toggle.error instanceof HttpError && toggle.error.reason ? `${toggle.error.reason}.` : 'The watch was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 19.8, Component: WatchHead }),
  PersonColumns.fill({ order: 19.8, Component: PersonWatch }),
];
