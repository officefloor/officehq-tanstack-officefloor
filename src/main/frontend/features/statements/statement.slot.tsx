import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { claimsKey } from '../claims/claims';
import { useUser } from '../session/session';
import { useRowError } from '../people/rowError';

function StatementHead() {
  return (
    <>
      <th>Statements</th>
      <th>Stated</th>
      <th>Stated on</th>
      <th />
    </>
  );
}

// A person's statements: how many have been taken, the last one's due-total and date, and a button
// that takes the next. Taking one binds each of their claims that counts to its figure now.
function PersonStatement({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useRowError(person.id, 'statement');
  const once = useOnce();
  const take = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/statement`,
        {},
        user,
      ),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]),
    onError: () => setError('That statement could not be taken.'),
  });

  const state = () => {
    setError('');
    once((done) => take.mutate(undefined, { onSettled: done }));
  };

  return (
    <>
      <td data-testid="person-statement-count">{person.statementCount}</td>
      <td data-testid="person-stated-total">{person.statedTotal}</td>
      <td data-testid="person-statement-date">{person.statementDate}</td>
      <td>
        <button type="button" data-testid="person-statement" onClick={state} disabled={take.isPending}>
          Statement
        </button>
        {error && (
          <p role="alert" data-testid="person-error">
            {error}
          </p>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 30, Component: StatementHead }),
  PersonRow.fill({ order: 30, Component: PersonStatement }),
];
