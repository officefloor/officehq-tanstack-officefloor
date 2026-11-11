import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useClaims } from '../claims/claims';
import type { ClaimStatus } from '../claims/claims';

// The statuses counted on each person's row.
const COUNTED: ClaimStatus[] = ['OPEN', 'SETTLED'];

function ClaimCountsHead() {
  return (
    <>
      {COUNTED.map((s) => (
        <th key={s}>{s === 'OPEN' ? 'Open' : 'Settled'}</th>
      ))}
    </>
  );
}

// How many of a person's claims are OPEN, and how many SETTLED.
function PersonClaimCounts({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  return (
    <>
      {COUNTED.map((s) => (
        <td key={s} data-testid={`person-claims-${s}-count`}>
          {claims.filter((c) => c.status === s).length}
        </td>
      ))}
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 26, Component: ClaimCountsHead }),
  PersonRow.fill({ order: 26, Component: PersonClaimCounts }),
];
