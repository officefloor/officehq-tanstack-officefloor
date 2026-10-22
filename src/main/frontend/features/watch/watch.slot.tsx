import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { usePersonError } from '../people/rowError';
import type { Person } from '../people/people';
import { useWatches, watchesKey } from './watch';

/** Whether the signed-in user watches the person, and the toggle that starts or stops it. A watched person's claim
 *  settled, approved or re-settled notifies each watcher (in the audit file). */
function PersonWatch({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const watched = (useWatches(sheetId, user).data ?? []).includes(person.id);
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const toggle = useMutation({
    mutationFn: (on: boolean) =>
      putJson(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/watch`, { user, watched: on }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: watchesKey(sheetId, user) });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : `Could not change watching ${person.name}.`),
    onSettled: once.done,
  });

  return (
    <td>
      Watched <span data-testid="person-watched">{watched ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="person-watch"
        disabled={toggle.isPending}
        onClick={() => {
          begin();
          if (!once.start()) return;
          toggle.mutate(!watched);
        }}
      >
        {watched ? 'Unwatch' : 'Watch'}
      </button>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 47, Component: PersonWatch });
