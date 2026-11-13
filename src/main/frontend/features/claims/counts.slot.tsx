import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { statusCount, useClaims } from './claims';

// How many of a person's claims are OPEN, and how many SETTLED.
function ClaimCountsHead() {
  return (
    <>
      <th>Open</th>
      <th>Settled</th>
    </>
  );
}

function PersonClaimCounts({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const mine = (claims.data ?? []).filter((c) => c.personId === person.id);
  return (
    <>
      <td data-testid="person-claims-OPEN-count">{statusCount(mine, 'OPEN')}</td>
      <td data-testid="person-claims-SETTLED-count">{statusCount(mine, 'SETTLED')}</td>
    </>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 13, Component: ClaimCountsHead }),
  PersonColumns.fill({ order: 13, Component: PersonClaimCounts }),
];
