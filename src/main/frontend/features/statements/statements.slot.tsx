import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { claimsKey } from '../claims/claims';
import { statementsKey, useStatements } from './statements';

function StatementsHead() {
  return (
    <>
      <th>Statements</th>
      <th>Stated total</th>
      <th>Stated on</th>
      <th />
    </>
  );
}

/** A person's statement count, their last statement's due-total and date, and the button that takes one: it
 *  records their due-total now and binds each of their claims that counts to the figure it has now. */
function PersonStatements({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const statements = useStatements(sheetId);
  const queryClient = useQueryClient();
  const mine = statements.data?.find((s) => s.personId === person.id);
  const take = useMutation({
    mutationFn: () =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/statements`, { by: user }),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: statementsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]),
  });

  return (
    <>
      <td data-testid="person-statement-count">{mine?.count ?? 0}</td>
      <td data-testid="person-stated-total">{mine?.dueTotal ?? ''}</td>
      <td data-testid="person-statement-date">{mine?.date ?? ''}</td>
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
        {take.isError && <span data-testid="person-error"> Could not take a statement for this person.</span>}
      </td>
    </>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 30, Component: StatementsHead }),
  PersonRow.fill({ order: 30, Component: PersonStatements }),
];
