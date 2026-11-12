import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, personActionKey, useLatestPersonAction } from '../people/people';
import { claimsKey } from '../claims/claims';
import { useCurrentSheet, useCurrentUser } from '../../session/session';

function StatementHead() {
  return <th>Statements</th>;
}

// A person's statements: how many were taken, the last one's due-total and date, and the button that
// takes one (binding their counting claims to their figures now). A refused statement shows its error
// until the next is taken.
function PersonStatement({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  const take = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/statement`, { user }),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]),
  });

  const latest = useLatestPersonAction(sheet, person.id);

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
          take.reset();
          take.mutate();
        }}
      >
        Statement
      </button>
      {take.isError && take.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {take.error instanceof HttpError && take.error.reason ? `${take.error.reason}.` : 'The statement was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 18, Component: StatementHead }),
  PersonColumns.fill({ order: 18, Component: PersonStatement }),
];
