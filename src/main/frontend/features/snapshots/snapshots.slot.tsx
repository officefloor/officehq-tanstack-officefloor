import { PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useSnapshots } from './snapshots';

/** The person's snapshot lines, oldest first: the day being left and their due-total on it. */
function PersonSnapshots({ sheetId, person }: { sheetId: string; person: Person }) {
  const lines = (useSnapshots(sheetId).data ?? []).filter((s) => s.personId === person.id);
  return (
    <td>
      <ul>
        {lines.map((s, i) => (
          <li key={i} data-testid="person-snapshot-line">
            {s.day} {s.dueTotal}
          </li>
        ))}
      </ul>
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 24, Component: PersonSnapshots });
