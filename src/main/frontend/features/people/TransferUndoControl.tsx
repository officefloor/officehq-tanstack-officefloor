import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from './usePersonAction';
import { type Person } from './usePeople';

// Undoes the transfer that brought the person to this sheet (a steward only, on the sheet's today they
// arrived): they are transferred back to the sheet they came from. A later day, anyone else, or a person who
// did not arrive by transfer is refused, which shows the person's error.
export function TransferUndoControl({ personId }: { personId: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const undo = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/transfer-undo`,
        { user },
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sheet', sheet] }),
  });
  const taken = usePersonAction(personId, () => undo.reset());

  return (
    <>
      <button
        type="button"
        data-testid="person-transfer-undo"
        onClick={() => {
          taken();
          once(() => undo.mutateAsync());
        }}
      >
        Undo transfer
      </button>
      {undo.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The transfer could not be undone.
        </span>
      )}
    </>
  );
}
