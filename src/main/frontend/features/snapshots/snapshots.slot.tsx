import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useSnapshots } from './snapshots';

function SnapshotsHead() {
  return <th>Snapshots</th>;
}

/** A person's snapshot lines: one per SNAPSHOT written for them, oldest first, as "<the day being left> <due-total>". */
function PersonSnapshots({ sheetId, person }: { sheetId: string; person: Person }) {
  const snapshots = useSnapshots(sheetId);
  const mine = (snapshots.data ?? []).filter((s) => s.personId === person.id);
  return (
    <td>
      {mine.map((s, i) => (
        <div key={i} data-testid="person-snapshot-line">
          {`${s.day} ${s.dueTotal}`}
        </div>
      ))}
    </td>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 95, Component: SnapshotsHead }),
  PersonRow.fill({ order: 95, Component: PersonSnapshots }),
];
