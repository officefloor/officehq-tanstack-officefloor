import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { claimsKey } from '../claims/useClaims';
import { peopleKey, type Person } from '../people/usePeople';

/**
 * A person's statements: the button to take one, which records their due-total now and binds each
 * of their counting claims to the figure it has now (the rest are unbound), how many have been
 * taken, and the last one's due-total and date (blank when none has been taken).
 */
function PersonStatement({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const take = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/statement`, { user }),
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]),
  });
  const taken = person.statementCount > 0;
  return (
    <>
      <td data-testid="person-statement-count">{person.statementCount}</td>
      <td data-testid="person-stated-total">{taken ? (person.statedTotal ?? 'n/a') : ''}</td>
      <td data-testid="person-statement-date">{taken ? (person.statementDate ?? '') : ''}</td>
      <td>
        <button type="button" data-testid="person-statement" disabled={take.isPending} onClick={() => take.mutate()}>
          Statement
        </button>
        {take.isError && (
          <span data-testid="person-error" role="alert">
            {' '}
            Could not take this statement.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 20,
    Component: () => (
      <>
        <th>Statements</th>
        <th>Last stated</th>
        <th>Stated on</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 20, Component: PersonStatement }),
];
