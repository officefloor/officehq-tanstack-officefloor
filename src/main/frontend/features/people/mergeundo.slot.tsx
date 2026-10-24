import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { claimsKey } from '../claims/claims';
import { PersonRow } from '../../slots/defs/personRow';
import { usePersonError } from './rowError';
import type { Person } from './people';

/** The button that undoes the last merge into the person (only the steward who made it, on the sheet-today it was
 *  made): the merged person is back at the end of the list with the claims that came from them and their own
 *  aliases. */
function PersonMergeUndo({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const undo = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/merge-undo`, { user }),
    onSuccess: async () => {
      // claims and aliases move back to the restored person: everything shown of the sheet follows, the claims
      // first so the restored person shows with the claims they took back
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not undo the last merge into ${person.name}.`),
    onSettled: once.done,
  });

  return (
    <td>
      <button
        type="button"
        data-testid="person-merge-undo"
        disabled={undo.isPending}
        onClick={() => {
          begin();
          if (!once.start()) return;
          undo.mutate();
        }}
      >
        Undo merge
      </button>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 48.5, Component: PersonMergeUndo });
