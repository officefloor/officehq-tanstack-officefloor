import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peoplePath, type Person } from './people';

// Whether the signed-in user watches a person, and the button that starts or stops it. Each user watches for
// themselves; a watcher is told (in the audit file) of each of the person's claims settled, approved or re-settled.
const watchedQueryKey = (sheetId: string, user: string) => ['sheet', sheetId, 'watched', user] as const;

/** The ids of the people the signed-in user watches on the sheet. */
function useWatched() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: watchedQueryKey(sheet ?? '', user ?? ''),
    queryFn: () =>
      getJson<number[]>(`/api/sheets/${encodeURIComponent(sheet ?? '')}/watched?user=${encodeURIComponent(encodeURIComponent(user ?? ''))}`),
    enabled: !!sheet && !!user,
  });
}

function WatchHeader() {
  return <th>Watched</th>;
}

function PersonWatch({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const watched = useWatched();
  const on = (watched.data ?? []).includes(person.id);
  const queryClient = useQueryClient();
  const watch = useMutation({
    mutationFn: (body: { on: boolean }) => putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/watch`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: watchedQueryKey(sheet ?? '', user ?? '') });
    },
  });
  const submit = useSubmitOnce(watch);
  const rowAction = useRowAction(`person-${person.id}`);
  return (
    <td>
      <span data-testid="person-watched">{on ? 'yes' : 'no'}</span>{' '}
      <button type="button" data-testid="person-watch" disabled={watch.isPending || !watched.isSuccess}
        onClick={() => { rowAction.take(); submit({ on: !on }); }}>
        {on ? 'Unwatch' : 'Watch'}
      </button>
      {watch.isError && rowAction.isLatest && <span data-testid="person-error">The watch could not be changed.</span>}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 48, Component: WatchHeader }),
  PersonCells.fill({ order: 48, Component: PersonWatch }),
];
