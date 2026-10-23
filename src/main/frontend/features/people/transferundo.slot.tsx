import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { usePersonError } from './rowError';
import type { Person } from './people';

/** The button that undoes the transfer that brought the person here (a steward only, on the sheet-today they
 *  arrived): they are transferred back to the sheet they came from, and leave the list. */
function PersonTransferUndo({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const undo = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/transfer-undo`, { user }),
    onSuccess: async () => {
      // the person and their claims go back to the sheet they came from: everything shown of both follows
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] }),
        ...(person.origin ? [queryClient.invalidateQueries({ queryKey: ['sheet', person.origin] })] : []),
      ]);
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not undo the transfer of ${person.name}.`),
    onSettled: once.done,
  });

  return (
    <td>
      <button
        type="button"
        data-testid="person-transfer-undo"
        disabled={undo.isPending}
        onClick={() => {
          begin();
          if (!once.start()) return;
          undo.mutate();
        }}
      >
        Undo transfer
      </button>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 50, Component: PersonTransferUndo });
