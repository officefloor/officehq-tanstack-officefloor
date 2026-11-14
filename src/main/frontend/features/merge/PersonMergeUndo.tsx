import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';

// The button that undoes the last merge into the person (only the steward who made it, on the sheet's today it was
// made): the merged person is back at the end of the list with the claims that came from them and their own aliases,
// so everything on the sheet is re-read.
export function PersonMergeUndo({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const undo = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/merge-undo?user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
    },
    onError: () => setError('The merge could not be undone.'),
  });

  const submit = () => {
    setError('');
    once(() => undo.mutateAsync());
  };

  return (
    <td>
      <button type="button" data-testid="person-merge-undo" disabled={undo.isPending} onClick={submit}>
        Undo merge
      </button>
    </td>
  );
}
