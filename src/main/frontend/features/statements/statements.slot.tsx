import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { claimsKey } from '../claims/useClaims';
import { peopleKey, usePeople, type Person } from '../people/people.slot';
import { useUser } from '../session/useSession';

function StatementHeads() {
  return (
    <>
      <th>Statements</th>
      <th>Last stated total</th>
      <th>Last statement</th>
      <th />
    </>
  );
}

// A person's statement count, the last statement's due-total and date, and the button that takes a statement:
// it records their due-total and binds their counting claims to the figures they have now. A refusal shows on
// the person until their next statement.
function StatementCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const person = usePeople(sheetId).data?.find((p) => p.id === personId);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const take = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/statements`, { by: user ?? '' }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });

  return (
    <>
      <td data-testid="person-statement-count">{person?.statementCount ?? 0}</td>
      <td data-testid="person-stated-total">{person?.statedTotal ?? ''}</td>
      <td data-testid="person-statement-date">{person?.statementDate ?? ''}</td>
      <td>
        <button type="button" data-testid="person-statement" disabled={take.isPending} onClick={() => take.mutate()}>
          Statement
        </button>
        {take.isError && (
          <span data-testid="person-error" role="alert">
            {take.error.message}
          </span>
        )}
      </td>
    </>
  );
}

// Whether the person's last statement bound the claim to its figure.
function ClaimStated({ claim }: { sheetId: string; user: string; personId: number; claim: { stated: boolean } }) {
  return <span data-testid="claim-stated">{claim.stated ? 'yes' : 'no'}</span>;
}

export const contributions = [
  PersonHeads.fill({ order: 30, Component: StatementHeads }),
  PersonCells.fill({ order: 30, Component: StatementCells }),
  ClaimCells.fill({ order: 15, Component: ClaimStated }),
];
