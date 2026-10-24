import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { claimsQueryKey } from '../claims/claims';
import { peoplePath, peopleQueryKey, type Person } from '../people/people';

// A person's statements: how many were taken, the last one's due-total and date, and the button that takes
// one — recording their due-total now and binding each of their counted claims to the figure it has now.
function StatementHeader() {
  return <th>Statements</th>;
}

function PersonStatement({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();
  const take = useMutation({
    mutationFn: () => putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/statement`, undefined, user),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') }),
        queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet ?? '') }),
      ]),
  });
  const submit = useSubmitOnce(take);
  const rowAction = useRowAction(`person-${person.id}`);
  return (
    <td>
      <span data-testid="person-statement-count">{person.statementCount}</span>{' '}
      <span data-testid="person-stated-total">{person.statedTotal ?? ''}</span>{' '}
      <span data-testid="person-statement-date">{person.statementDate ?? ''}</span>{' '}
      <button type="button" data-testid="person-statement" disabled={take.isPending} onClick={() => { rowAction.take(); submit(undefined); }}>
        Statement
      </button>
      {take.isError && rowAction.isLatest && <span data-testid="person-error">The statement could not be taken.</span>}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 30, Component: StatementHeader }),
  PersonCells.fill({ order: 30, Component: PersonStatement }),
];
