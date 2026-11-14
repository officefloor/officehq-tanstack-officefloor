import type { Person } from '../people/people';
import { useSnapshots } from './snapshots';

// A person's snapshot lines, oldest first: one per SNAPSHOT written for them, as the day being left and their
// due-total on it.
export function PersonSnapshots({ person }: { person: Person }) {
  const snapshots = (useSnapshots().data ?? []).filter((s) => s.personId === person.id);
  return (
    <td>
      {snapshots.map((s, i) => (
        <div key={i} data-testid="person-snapshot-line">
          {s.day} {s.dueTotal}
        </div>
      ))}
    </td>
  );
}
