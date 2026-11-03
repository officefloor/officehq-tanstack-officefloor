import { useCallback, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { claimsKey } from '../claims/useClaims';
import { peopleKey, usePeople, type Person } from '../people/usePeople';
import { usePersonRowAction } from '../people/usePersonRowAction';
import { useFigure } from '../locale/useLocale';

/** A person's statements: how many have been taken, the last one's due-total and date (blank before
 *  the first), and a button that takes one. Taking a statement records their due-total and binds each
 *  of their counting claims to the figure it has then (unbinding the rest), until the next one; a
 *  steward can cancel the last one, its claims going live again. A date beside the button takes the
 *  statement as of that date (on or before the sheet's today; blank: now); it is cleared once that statement is
 *  taken or refused. */
function PersonStatement({ sheetId, personId }: { sheetId: string; personId: number }) {
  const fig = useFigure();
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const p = people.data?.find((x) => x.id === personId);
  const [asof, setAsof] = useState('');

  const take = useMutation({
    mutationFn: (date: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/statement`, { asof: date }),
    // The date is for the one statement it was typed for: taken or refused, the next one is now unless a
    // date is typed again.
    onSettled: () => setAsof(''),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });
  const unstate = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/unstate`, {}),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });
  const { reset: resetTake } = take;
  const { reset: resetUnstate } = unstate;
  const reset = useCallback(() => {
    resetTake();
    resetUnstate();
  }, [resetTake, resetUnstate]);
  const announce = usePersonRowAction(sheetId, personId, reset);
  const error = take.error ?? unstate.error;

  return (
    <>
      <td data-testid="person-statement-count">{p?.statementCount ?? 0}</td>
      <td data-testid="person-stated-total">{fig(p?.statedTotal ?? '')}</td>
      <td data-testid="person-statement-date">{p?.statementDate ?? ''}</td>
      <td>
        <input
          type="text"
          placeholder="yyyy-mm-dd"
          data-testid="person-statement-asof"
          aria-label="Statement as of"
          value={asof}
          onChange={(e) => {
            setAsof(e.target.value);
            reset();
          }}
        />
        <button
          type="button"
          data-testid="person-statement"
          disabled={take.isPending}
          onClick={() => {
            announce();
            reset();
            take.mutate(asof);
          }}
        >
          Statement
        </button>
        <button
          type="button"
          data-testid="person-unstate"
          disabled={unstate.isPending}
          onClick={() => {
            announce();
            reset();
            unstate.mutate();
          }}
        >
          Cancel statement
        </button>
        {error && <span data-testid="person-error">{error.message}</span>}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHead.fill({
    order: 30,
    Component: () => (
      <>
        <th>Statements</th>
        <th>Stated total</th>
        <th>Stated on</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 30, Component: PersonStatement }),
];
