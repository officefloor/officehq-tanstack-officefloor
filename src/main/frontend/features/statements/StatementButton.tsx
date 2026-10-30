import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from '../people/usePersonAction';
import { claimsKey } from '../claims/useClaims';
import { peopleKey, type Person } from '../people/usePeople';

// Takes a statement for the person: the server records their due-total now and binds each of their
// counting claims to its figure now (the rest are left unbound). With a date beside the button (on or
// before the sheet's today; blank: now) the statement is taken as of the end of that date. A refusal
// shows the person's error.
export function StatementButton({ personId }: { personId: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [asOf, setAsOf] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const take = useMutation({
    mutationFn: (date: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/statement`, {
        user,
        asOf: date,
      }),
    onSuccess: () => {
      setAsOf('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });
  const taken = usePersonAction(personId, () => take.reset());

  return (
    <>
      <input
        type="date"
        data-testid="person-statement-asof"
        value={asOf}
        title="Statement as of (blank: now)"
        onChange={(e) => {
          setAsOf(e.target.value);
          take.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-statement"
        onClick={() => {
          taken();
          once(() => take.mutateAsync(asOf.trim()));
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
