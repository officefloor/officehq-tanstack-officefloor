import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useIsLatest } from '../../query/useIsLatest';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peoplePath, personActionKey } from '../people/people';
import type { Person } from '../people/people';
import { useWatches, watchesKey } from './watches';

function WatchHeader() {
  return (
    <>
      <th>Watched</th>
      <th />
    </>
  );
}

// Whether the signed-in user watches the person, and the toggle that starts or stops it. Each user has their own
// watches; a watcher is told (in the audit file) when one of the person's claims is settled, approved or re-settled.
function PersonWatch({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: watched = [] } = useWatches(sheet, user);
  const on = watched.includes(person.id);
  const watch = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (to: boolean) =>
      postJson<number[]>(byUser(`${peoplePath(sheet)}/${person.id}/watch`, user), { on: to }),
    onSuccess: (updated) => queryClient.setQueryData(watchesKey(sheet, user), updated),
  });
  const act = useSubmitOnce(watch);
  const latest = useIsLatest(personActionKey(sheet, person.id), watch.submittedAt);

  return (
    <>
      <td data-testid="person-watched">{on ? 'yes' : 'no'}</td>
      <td>
        <button type="button" data-testid="person-watch" disabled={watch.isPending} onClick={() => act(!on)}>
          {on ? 'Unwatch' : 'Watch'}
        </button>
        {watch.isError && latest && (
          <span data-testid="person-error" role="alert">
            {' '}That watch was refused.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 60, Component: WatchHeader }),
  PersonColumns.fill({ order: 60, Component: PersonWatch }),
];
