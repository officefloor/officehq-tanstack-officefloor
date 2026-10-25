import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import type { Person } from '../people/people';
import { useClaims, type ClaimStatus } from './claims';

/** The statuses whose claims are counted per person, each in its own column. */
const COUNTED: ClaimStatus[] = ['OPEN', 'SETTLED'];

// A person's count of claims in each counted status (OPEN, SETTLED), as the store holds them.
function StatusCountsHeader() {
  return <>{COUNTED.map((s) => <th key={s}>{s === 'OPEN' ? 'Open' : 'Settled'}</th>)}</>;
}

function PersonStatusCounts({ person }: { person: Person }) {
  const claims = useClaims();
  const mine = (claims.data ?? []).filter((c) => c.personId === person.id);
  return (
    <>
      {COUNTED.map((s) => (
        <td key={s} data-testid={`person-claims-${s}-count`}>{mine.filter((c) => c.status === s).length}</td>
      ))}
    </>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 10.2, Component: StatusCountsHeader }),
  PersonCells.fill({ order: 10.2, Component: PersonStatusCounts }),
];
