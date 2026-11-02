import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { claimsKey } from '../claims/useClaims';
import { peopleKey, usePeople, type Person } from '../people/usePeople';

/** A person's statements: how many have been taken, the last one's due-total and date (blank before
 *  the first), and a button that takes one. Taking a statement records their due-total and binds each
 *  of their counting claims to the figure it has then (unbinding the rest), until the next one. */
function PersonStatement({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const p = people.data?.find((x) => x.id === personId);

  const take = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/statement`, {}),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });

  return (
    <>
      <td data-testid="person-statement-count">{p?.statementCount ?? 0}</td>
      <td data-testid="person-stated-total">{p?.statedTotal ?? ''}</td>
      <td data-testid="person-statement-date">{p?.statementDate ?? ''}</td>
      <td>
        <button
          type="button"
          data-testid="person-statement"
          disabled={take.isPending}
          onClick={() => {
            take.reset();
            take.mutate();
          }}
        >
          Statement
        </button>
        {take.isError && <span data-testid="person-error">{take.error.message}</span>}
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
