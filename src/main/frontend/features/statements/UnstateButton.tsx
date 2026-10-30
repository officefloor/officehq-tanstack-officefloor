import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from '../people/usePersonAction';
import { claimsKey } from '../claims/useClaims';
import { peopleKey, type Person } from '../people/usePeople';

// Cancels the person's last statement: the server frees the claims it bound and shows the statement
// before it. Only a steward may, and only for a person with a statement; a refusal shows the person's error.
export function UnstateButton({ personId }: { personId: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const cancel = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/unstate`, { user }),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]),
  });
  const taken = usePersonAction(personId, () => cancel.reset());

  return (
    <>
      <button
        type="button"
        data-testid="person-unstate"
        onClick={() => {
          taken();
          once(() => cancel.mutateAsync());
        }}
      >
        Cancel statement
      </button>
      {cancel.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The statement could not be cancelled.
        </span>
      )}
    </>
  );
}
