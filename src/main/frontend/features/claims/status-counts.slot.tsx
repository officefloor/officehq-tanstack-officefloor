import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { statusCount, useClaims, type ClaimStatus } from './claims';

/** The statuses counted on each person's row, in the order shown. */
const COUNTED: readonly ClaimStatus[] = ['OPEN', 'SETTLED'];

function StatusCountsHeader() {
  return <th>Open / Settled</th>;
}

// How many of the person's claims are OPEN and how many SETTLED (a PENDING claim counts in neither).
function PersonStatusCounts({ sheet, person }: { sheet: string; user: string; person: Person }) {
  const { data: claims = [] } = useClaims(sheet);
  const theirs = claims.filter((c) => c.person === person.id);
  return (
    <td>
      {COUNTED.map((s, i) => (
        <span key={s}>
          {i > 0 && ' / '}
          <span data-testid={`person-claims-${s}-count`}>{statusCount(theirs, s)}</span>
        </span>
      ))}
    </td>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 6, Component: StatusCountsHeader }),
  PersonColumns.fill({ order: 6, Component: PersonStatusCounts }),
];
