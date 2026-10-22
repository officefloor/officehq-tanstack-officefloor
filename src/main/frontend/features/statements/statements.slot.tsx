import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonRow } from '../../slots/defs/personRow';
import { claimsKey } from '../claims/claims';
import { peopleKey } from '../people/people';
import { usePersonError } from '../people/rowError';
import type { Person } from '../people/people';

/** A person's statements: how many have been taken, the last one's due-total and date, and the button that
 *  takes one (recording their due-total now and binding their claims that count to the figures they show; or, with a
 *  date in the as-of entry beside it, as they stood at the end of that date), and the
 *  button that cancels the last one (a steward only: its claims are live again; the row shows the one before it). */
function PersonStatement({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const [asOf, setAsOf] = useState('');

  const take = useMutation({
    mutationFn: (date: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/statements`,
        date ? { user, asOf: date } : { user }),
    onSettled: () => setAsOf(''), // the as-of entry is for one statement: taken or refused, it is cleared
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
      <input
        type="date"
        aria-label="Statement as of"
        data-testid="person-statement-asof"
        value={asOf}
        onChange={(e) => {
          begin();
          setAsOf(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-statement"
        disabled={take.isPending || cancel.isPending}
        onClick={() => {
          begin();
          take.mutate(asOf.trim());
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
