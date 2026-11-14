import { useClaims } from '../claims/claims';
import type { ClaimStatus } from '../claims/claims';
import type { Person } from '../people/people';

/** The statuses whose claims are counted on each person, in column order. */
export const countedStatuses: ClaimStatus[] = ['OPEN', 'SETTLED'];

// How many of a person's claims are OPEN and how many SETTLED: one cell each.
export function PersonClaimCounts({ person }: { person: Person }) {
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return (
    <>
      {countedStatuses.map((s) => (
        <td key={s} data-testid={`person-claims-${s}-count`}>
          {claims.filter((c) => c.status === s).length}
        </td>
      ))}
    </>
  );
}
