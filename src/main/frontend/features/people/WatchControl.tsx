import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from './usePersonAction';
import { type Person } from './usePeople';
import { watchesKey } from './useWatches';

// Starts or stops the signed-in user watching the person (watching is per user). When a watched
// person's claim is settled, approved or re-settled, each watcher is notified. A refusal shows the
// person's error.
export function WatchControl({ personId, watched }: { personId: string; watched: boolean }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const watch = useMutation({
    mutationFn: (on: boolean) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/watch`, {
        on,
        user,
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: watchesKey(sheet, user) }),
  });
  const taken = usePersonAction(personId, () => watch.reset());

  return (
    <>
      <button
        type="button"
        data-testid="person-watch"
        onClick={() => {
          taken();
          once(() => watch.mutateAsync(!watched));
        }}
      >
        {watched ? 'Unwatch' : 'Watch'}
      </button>
      {watch.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The watch could not be changed.
        </span>
      )}
    </>
  );
}
