import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { claimsKey } from '../claims/claims';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { useCurrentSheet, useCurrentUser } from '../session/session';

// A person's statements: how many were taken, the last one's due-total and date, and the button that
// takes one — recording their due-total now and binding each counting claim to its figure now — and the
// button a steward cancels the last one with, making the claims it bound live again.
export function PersonStatement({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const take = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/statement?user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
    onError: () => setError('The statement could not be taken.'),
  });

  const cancel = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/statement-cancel?user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
    onError: () => setError('The statement could not be cancelled.'),
  });

  return (
    <td>
      <span data-testid="person-statement-count">{person.statementCount}</span>{' '}
      <span data-testid="person-stated-total">{person.statedTotal ?? ''}</span>{' '}
      <span data-testid="person-statement-date">{person.statementDate ?? ''}</span>{' '}
      <button
        type="button"
        data-testid="person-statement"
        disabled={take.isPending}
        onClick={() => {
          setError('');
          once(() => take.mutateAsync());
        }}
      >
        Statement
      </button>
      <button
        type="button"
        data-testid="person-unstate"
        disabled={cancel.isPending}
        onClick={() => {
          setError('');
          once(() => cancel.mutateAsync());
        }}
      >
        Cancel statement
      </button>
    </td>
  );
}
