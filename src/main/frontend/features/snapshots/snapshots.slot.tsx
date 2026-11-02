import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useSnapshotLines } from './snapshots';

function SnapshotHeader() {
  return <th>Snapshots</th>;
}

// The person's snapshot lines: one per SNAPSHOT written for them as the sheet's today moved, oldest first, as the
// day being left and their due-total on it.
function PersonSnapshots({ sheet, person }: { sheet: string; user: string; person: Person }) {
  const { data: lines = [] } = useSnapshotLines(sheet);
  return (
    <td>
      {lines
        .filter((l) => l.person === person.id)
        .map((l, i) => (
          <div key={i} data-testid="person-snapshot-line">
            {l.day} {l.dueTotal}
          </div>
        ))}
    </td>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 80, Component: SnapshotHeader }),
  PersonColumns.fill({ order: 80, Component: PersonSnapshots }),
];
