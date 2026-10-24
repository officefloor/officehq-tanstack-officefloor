import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import type { Person } from './people';

// A person's snapshot lines: one per SNAPSHOT written for them as the sheet's today moved on, oldest first, as the
// day being left and their due-total on it.
function SnapshotsHeader() {
  return <th>Snapshots</th>;
}

function PersonSnapshots({ person }: { person: Person }) {
  return (
    <td>
      {person.snapshots.map((s, i) => (
        <div key={i} data-testid="person-snapshot-line">{s.date} {s.dueTotal}</div>
      ))}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 40.5, Component: SnapshotsHeader }),
  PersonCells.fill({ order: 40.5, Component: PersonSnapshots }),
];
