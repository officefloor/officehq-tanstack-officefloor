import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from './usePersonAction';
import { type Person } from './usePeople';

// Undoes the last merge into the person (a steward only, the one who merged, on the sheet's today it was
// made): the merged person is back at the end of the list with the claims that came from them and their own
// aliases. A later day, anyone else, or a person with no merge to undo is refused, which shows the person's error.
export function MergeUndoControl({ personId }: { personId: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const undo = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/merge-undo`,
        { user },
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sheet', sheet] }),
  });
  const taken = usePersonAction(personId, () => undo.reset());

  return (
    <>
      <button
        type="button"
        data-testid="person-merge-undo"
        onClick={() => {
          taken();
          once(() => undo.mutateAsync());
        }}
      >
        Undo merge
      </button>
      {undo.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The merge could not be undone.
        </span>
      )}
    </>
  );
}
