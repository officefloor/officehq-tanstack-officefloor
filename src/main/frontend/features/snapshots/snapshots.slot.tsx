import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { usePeople } from '../people/people.slot';

function SnapshotHeads() {
  return <th>Snapshots</th>;
}

// The person's snapshot lines, oldest first: one per SNAPSHOT written for them as the sheet's today moved on, the
// day being left and their due-total as it stood then.
function SnapshotCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const person = usePeople(sheetId).data?.find((p) => p.id === personId);
  return (
    <td>
      {(person?.snapshots ?? []).map((line, i) => (
        <div key={i} data-testid="person-snapshot-line">
          {line}
        </div>
      ))}
    </td>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 21.5, Component: SnapshotHeads }),
  PersonCells.fill({ order: 21.5, Component: SnapshotCells }),
];
