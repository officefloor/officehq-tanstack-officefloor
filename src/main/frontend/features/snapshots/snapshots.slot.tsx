import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import type { Person } from '../people/usePeople';

/**
 * A person's snapshot lines: one per SNAPSHOT written for them when the sheet's today moved, oldest
 * first, each the day being left and their due-total then (n/a when it was n/a).
 */
function PersonSnapshots({ person }: { user: string; sheet: string; person: Person }) {
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
  PersonColumns.fill({ order: 36, Component: () => <th>Snapshots</th> }),
  PersonCells.fill({ order: 36, Component: PersonSnapshots }),
];
