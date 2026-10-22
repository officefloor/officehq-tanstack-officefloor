import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonRow } from '../../slots/defs/personRow';
import { claimsKey } from '../claims/claims';
import { peopleKey } from '../people/people';
import { usePersonError } from '../people/rowError';
import type { Person } from '../people/people';

/** A person's statements: how many have been taken, the last one's due-total and date, and the button that
 *  takes one (recording their due-total now and binding their claims that count to the figures they show), and the
 *  button that cancels the last one (a steward only: its claims are live again; the row shows the one before it). */
function PersonStatement({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { error, setError, begin } = usePersonError(sheetId, person.id);

  const take = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/statements`, { user }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not take a statement of ${person.name}.`),
  });

  const cancel = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/statements/cancel`, { user }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not cancel the statement of ${person.name}.`),
  });

  const taken = person.statementCount > 0;
  return (
    <td>
      Statements <span data-testid="person-statement-count">{person.statementCount}</span>{' '}
      last <span data-testid="person-stated-total">{taken ? (person.statedTotal ?? 'n/a') : ''}</span>{' '}
      on <span data-testid="person-statement-date">{person.statementDate ?? ''}</span>{' '}
      <button
        type="button"
        data-testid="person-statement"
        disabled={take.isPending || cancel.isPending}
        onClick={() => {
          begin();
          take.mutate();
        }}
      >
        Statement
      </button>{' '}
      <button
        type="button"
        data-testid="person-unstate"
        disabled={cancel.isPending}
        onClick={() => {
          begin();
          cancel.mutate();
        }}
      >
        Cancel statement
      </button>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 30, Component: PersonStatement });
