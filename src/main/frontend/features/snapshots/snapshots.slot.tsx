import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';

function SnapshotsHead() {
  return <th>Snapshots</th>;
}

// A person's snapshot lines: one per SNAPSHOT written for them when the sheet's today changed,
// oldest first, each as '<the day being left> <due-total>'.
function PersonSnapshots({ person }: { sheetId: string; person: Person }) {
  return (
    <td>
      {person.snapshots.map((line, i) => (
        <div key={i} data-testid="person-snapshot-line">
          {line}
        </div>
      ))}
    </td>
  );
}

export const contributions = [
  PersonHead.fill({ order: 67, Component: SnapshotsHead }),
  PersonRow.fill({ order: 67, Component: PersonSnapshots }),
];
