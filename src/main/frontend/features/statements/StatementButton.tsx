import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from '../people/usePersonAction';
import { claimsKey } from '../claims/useClaims';
import { peopleKey, type Person } from '../people/usePeople';

// Takes a statement for the person: the server records their due-total now and binds each of their
// counting claims to its figure now (the rest are left unbound). A refusal shows the person's error.
export function StatementButton({ personId }: { personId: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const take = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/statement`, { user }),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]),
  });
  const taken = usePersonAction(personId, () => take.reset());

  return (
    <>
      <button
        type="button"
        data-testid="person-statement"
        onClick={() => {
          taken();
          once(() => take.mutateAsync());
        }}
      >
        Statement
      </button>
      {take.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The statement could not be taken.
        </span>
      )}
    </>
  );
}
