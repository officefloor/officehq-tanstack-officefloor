import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';

// The button that undoes the transfer that brought the person here (a steward only, on the sheet's today they
// arrived): they go back to the sheet they came from with their claims, so everything on the sheet is re-read.
export function PersonTransferUndo({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const undo = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/transfer-undo?user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
    },
    onError: () => setError('The transfer could not be undone.'),
  });

  const submit = () => {
    setError('');
    once(() => undo.mutateAsync());
  };

  return (
    <td>
      <button type="button" data-testid="person-transfer-undo" disabled={undo.isPending} onClick={submit}>
        Undo transfer
      </button>
    </td>
  );
}
